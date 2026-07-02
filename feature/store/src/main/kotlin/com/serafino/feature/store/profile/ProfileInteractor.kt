package com.serafino.feature.store.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.serafino.architecture.AnalyticsEvent
import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.AppDidBecomeActiveEvent
import com.serafino.architecture.AuthChangedEvent
import com.serafino.architecture.AuthError
import com.serafino.architecture.AuthProviding
import com.serafino.architecture.BeansEarnedEvent
import com.serafino.architecture.EventBus
import com.serafino.architecture.Interactor
import com.serafino.architecture.NoOpAnalytics
import com.serafino.architecture.PurchaseCompletedEvent
import com.serafino.architecture.RewardRedeemedEvent
import com.serafino.architecture.events
import com.serafino.domain.entities.loyalty.LoyaltyAccount
import com.serafino.domain.entities.loyalty.LoyaltyReward
import com.serafino.domain.entities.loyalty.LoyaltyTier
import com.serafino.domain.services.LoyaltyProviding
import com.serafino.domain.services.Money
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Controla la sesión y la cuenta de fidelidad del Perfil. Sin sesión expone el gate; con sesión
 * carga la cuenta y la mapea a datos para la vista. Espeja `ProfileInteractor` de iOS.
 *
 * Frescura: los triggers de ciclo de vida (reingreso al tab, vuelta al foreground) refrescan
 * en segundo plano con la carga LIVIANA (`loadAccountSummary`, 1 GET) y solo si el TTL venció;
 * mientras el tab está visible, [autoRefresh] repite esa carga liviana cada
 * [autoRefreshInterval] (el `LaunchedEffect` de la pantalla cancela el bucle al salir del tab);
 * las mutaciones (compra aprobada, canje) fuerzan la carga completa — la compra con reintentos
 * escalonados que cortan al ver el saldo acreditado (la ventana del webhook de Mercado Pago).
 * Las cargas concurrentes comparten una sola request (dedupe en vuelo).
 */
class ProfileInteractor(
    private val auth: AuthProviding,
    private val loyalty: LoyaltyProviding,
    private val bus: EventBus,
    private val analytics: AnalyticsTracking = NoOpAnalytics(),
    /** Cada cuánto se auto-refresca la cuenta mientras el Perfil está visible. */
    private val autoRefreshInterval: Duration = 5.seconds,
    /** Ventana de frescura: reingresos al tab / foreground dentro de este TTL no tocan la red. */
    private val staleTTL: Duration = 15.seconds,
    /**
     * Esperas de la reconciliación post-compra (la acreditación llega por webhook y puede
     * tardar): se reintenta con estos delays y se corta apenas el saldo cambia.
     */
    private val reconcileDelays: List<Duration> = listOf(2.seconds, 5.seconds, 15.seconds),
    private val now: () -> Date = ::Date,
) : Interactor<ProfileInteractor.Data, ProfileInteractor.Input, ProfileInteractor.State> {

    data class ActivityRow(
        val id: String,
        val orderLabel: String?,
        val orderId: String?,
        val typeText: String,
        val dateText: String,
        val amountText: String,
        val isCredit: Boolean,
        val symbol: String,
    )

    data class Data(
        val memberName: String = "",
        val memberSinceText: String = "",
        val pointsName: String = "granos",
        val balanceText: String = "0",
        val tier: LoyaltyTier = LoyaltyTier.placeholder,
        val nextTierName: String? = null,
        val beansToNextText: String = "",
        val progress: Double = 0.0,
        val isMaxTier: Boolean = false,
        val tiers: List<LoyaltyTier> = emptyList(),
        val unlockedTierIDs: Set<String> = emptySet(),
        val perks: List<String> = emptyList(),
        val nextPerks: List<String> = emptyList(),
        val activity: List<ActivityRow> = emptyList(),
        val howToEarnText: String = "",
        val signInError: String? = null,
    )

    sealed interface Input {
        data object OnAppear : Input
        data object SignIn : Input
        data object SignOut : Input
        data object Retry : Input
    }

    sealed interface State {
        data object SignedOut : State
        data object SigningIn : State
        data object Loading : State
        data object Loaded : State
        data class Error(val message: String) : State
    }

    /**
     * Qué tan completo es un refresco: [Full] trae escalera + historial; [Summary] reusa
     * los cacheados por el provider y pide solo el estado (los triggers frecuentes).
     */
    private enum class LoadKind { Full, Summary }

    override var data by mutableStateOf(Data())
        private set
    override var state by mutableStateOf<State>(State.SignedOut)
        private set

    /** Última carga exitosa de la cuenta: gatea los refrescos de ciclo de vida (TTL). */
    var lastUpdatedAt: Date? = null
        private set

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    /**
     * Carga en vuelo (con su tipo): los triggers concurrentes (AuthChanged + onAppear del
     * arranque, doble evento de un canje) la comparten en vez de duplicar los GETs. El
     * tipo importa: un `Full` (post-mutación) NO puede conformarse con un `Summary` en
     * vuelo — su request pudo despacharse antes de la mutación y traer datos viejos.
     */
    private var loadTask: Pair<LoadKind, Job>? = null
    private var reconcileTask: Job? = null

    /** Saldo crudo de la última cuenta cargada (para detectar la acreditación post-compra). */
    private var points = 0

    init {
        bindEvents()
    }

    override fun handle(input: Input) {
        when (input) {
            is Input.OnAppear -> {
                analytics.track(AnalyticsEvent.Screen("profile"))
                evaluateSession(initialLoad = true)
            }
            is Input.SignIn -> signIn()
            is Input.SignOut -> runCatching { auth.signOut() }
            is Input.Retry -> load()
        }
    }

    fun dispose() = scope.cancel()

    /**
     * Pull-to-refresh: vuelve a pedir la cuenta COMPLETA (ignora el TTL) sin parpadeo. El
     * control nativo ya muestra su spinner, así que no pasamos por `Loading`. Sin sesión no
     * hay nada que refrescar.
     */
    suspend fun refresh() {
        if (auth.currentUser == null) return
        performLoad(LoadKind.Full)
    }

    /**
     * Mientras el Perfil esté en pantalla, refresca la cuenta en segundo plano (sin parpadeo)
     * cada [autoRefreshInterval], con la carga LIVIANA (1 GET). El `LaunchedEffect` de la
     * pantalla cancela este bucle al salir del tab, así no consultamos al backend desde otras
     * pestañas. Sin sesión o sin contenido cargado, el tick no hace nada (el gate no tiene qué
     * refrescar) pero el bucle sigue por si el usuario inicia sesión con el tab abierto.
     */
    suspend fun autoRefresh() {
        while (currentCoroutineContext().isActive) {
            delay(autoRefreshInterval)
            if (auth.currentUser == null || state !is State.Loaded) continue
            performLoad(LoadKind.Summary)
        }
    }

    /** Decide gate vs contenido según haya sesión. Carga la cuenta solo si hace falta. */
    private fun evaluateSession(initialLoad: Boolean = false) {
        if (auth.currentUser != null) {
            // Con sesión: la primera vez mostramos el skeleton (`load`). Si ya hay contenido en
            // pantalla y reingresamos al Perfil, refrescamos en segundo plano (sin parpadeo y
            // solo si el TTL venció) para mantener los servicios al día sin gastar requests.
            if (initialLoad && state is State.Loaded) {
                refreshIfStale()
                return
            }
            // AuthChanged con contenido ya cargado (p. ej. re-publicación del listener de
            // sesión): no volvemos al skeleton, refrescamos en silencio.
            if (state is State.Loaded) {
                scope.launch { performLoad(LoadKind.Full) }
                return
            }
            load()
        } else {
            // Sin sesión: solo el gate. Limpia el contenido del usuario anterior.
            clear()
            state = State.SignedOut
        }
    }

    private fun signIn() {
        data = data.copy(signInError = null)
        state = State.SigningIn
        scope.launch {
            runCatching { auth.signIn() }
                .onSuccess { evaluateSession() }
                .onFailure { error ->
                    state = State.SignedOut
                    data = if (error is AuthError.Cancelled) data.copy(signInError = null)
                    else data.copy(signInError = (error as? AuthError)?.displayMessage ?: error.message)
                }
        }
    }

    // MARK: - Carga de fidelidad

    private val isStale: Boolean
        get() {
            val last = lastUpdatedAt ?: return true
            return now().time - last.time > staleTTL.inWholeMilliseconds
        }

    /**
     * Refresco en segundo plano gateado por TTL (reingreso al tab / vuelta al foreground).
     * Usa la carga liviana: la escalera y el historial cambian con mutaciones, no solos.
     */
    private fun refreshIfStale() {
        if (!isStale) return
        scope.launch { performLoad(LoadKind.Summary) }
    }

    private fun load() {
        state = State.Loading
        scope.launch { performLoad(LoadKind.Full) }
    }

    private suspend fun performLoad(kind: LoadKind) {
        // Dedupe en vuelo: un trigger que llega con otra carga corriendo espera esa misma
        // request en vez de duplicarla (AuthChanged + onAppear del arranque, doble evento de
        // canje). Un `Summary` se conforma con cualquier carga; un `Full` solo con otra
        // `Full` — si lo que corre es un summary, espera y relanza la carga completa
        // (el historial y la escalera del summary pueden ser anteriores a la mutación).
        // OJO: un solo join, sin loop — joinear un job ya completado puede resumir sin
        // suspender, y un `while` acá gira para siempre sin dejar limpiar al dueño.
        loadTask?.let { inFlight ->
            inFlight.second.join()
            if (kind == LoadKind.Summary || inFlight.first == LoadKind.Full) return
            // El dueño del summary puede no haber limpiado aún (resume después que nosotros):
            // limpiamos nosotros para poder relanzar la carga completa.
            if (loadTask?.second === inFlight.second) {
                loadTask = null
            } else {
                val relaunched = loadTask
                if (relaunched != null && relaunched.first == LoadKind.Full) {
                    // Otro trigger ya relanzó la completa mientras esperábamos (los dos eventos
                    // de un canje sobre un summary en vuelo): nos sumamos en vez de duplicarla.
                    relaunched.second.join()
                    return
                }
            }
        }
        val uid = auth.currentUser?.uid ?: run { state = State.SignedOut; return }
        val job = scope.launch {
            try {
                val account =
                    if (kind == LoadKind.Summary) loyalty.loadAccountSummary() else loyalty.loadAccount()
                // La sesión pudo cerrarse o cambiar durante la request: no resucitar
                // datos del usuario anterior pisando el gate.
                if (auth.currentUser?.uid != uid) return@launch
                build(account)
                lastUpdatedAt = now()
                state = State.Loaded
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (auth.currentUser?.uid != uid) return@launch
                if (data.tiers.isEmpty()) {
                    state = State.Error(error.message ?: "No se pudo cargar.")
                }
            }
        }
        loadTask = kind to job
        job.join()
        // Solo el dueño limpia: si otra carga se relanzó mientras esperábamos, no pisarla.
        if (loadTask?.second === job) loadTask = null
    }

    /**
     * Tras una compra aprobada, la acreditación de granos llega por webhook (puede tardar):
     * reintenta la carga completa con esperas escalonadas y corta apenas el saldo cambia.
     */
    private fun reconcileAfterPurchase() {
        reconcileTask?.cancel()
        val before = points
        reconcileTask = scope.launch {
            for (wait in reconcileDelays) {
                delay(wait)
                if (auth.currentUser == null) return@launch
                performLoad(LoadKind.Full)
                if (points != before) return@launch   // acreditado: no seguimos pegando al backend
            }
        }
    }

    private fun build(account: LoyaltyAccount) {
        points = account.points
        val user = auth.currentUser
        val memberSince = user?.memberSince?.let { "Miembro desde ${monthYear.format(it)}" } ?: ""
        val singular = if (account.pointsName.endsWith("s")) account.pointsName.dropLast(1) else account.pointsName
        data = data.copy(
            memberName = user?.displayName ?: "Miembro Serafino",
            memberSinceText = memberSince,
            pointsName = account.pointsName,
            balanceText = grouped(account.points),
            howToEarnText = "Ganás 1 $singular por cada ${Money.ars(account.amountPerPoint)} en tus compras. Cuantos más ${account.pointsName} juntás, más beneficios desbloqueás.",
            tier = account.displayTier,
            nextTierName = account.nextTierLabel ?: account.nextTier?.name,
            isMaxTier = account.isMaxTier,
            progress = account.progress,
            tiers = account.sortedTiers,
            unlockedTierIDs = account.sortedTiers.filter { account.isUnlocked(it) }.map { it.id }.toSet(),
            perks = benefits(account.currentTier, account.rewards),
            nextPerks = account.nextTier?.let { benefits(it, account.rewards) } ?: emptyList(),
            beansToNextText = account.toNextLabel
                ?: if (account.isMaxTier) "Alcanzaste el nivel más alto" else "Te faltan ${grouped(account.beansToNextTier)} ${account.pointsName}",
            activity = account.history.sortedByDescending { it.date }.map { tx ->
                ActivityRow(
                    id = tx.id,
                    orderLabel = tx.orderLabel,
                    orderId = tx.orderId,
                    typeText = tx.kind.label,
                    dateText = dayMonth.format(tx.date),
                    amountText = (if (tx.isCredit) "+" else "") + grouped(tx.amount),
                    isCredit = tx.isCredit,
                    symbol = tx.kind.symbolName,
                )
            },
        )
    }

    private fun benefits(tier: LoyaltyTier, rewards: List<LoyaltyReward>): List<String> {
        val out = mutableListOf<String>()
        tier.cafeDelDiaPct?.let { out.add("Café del Día con $it% de descuento") }
        out.addAll(rewards.filter { it.unlockTierKey == tier.id }.map { "Desbloqueás ${it.label}" })
        return out.ifEmpty { tier.perks }
    }

    /** Limpia el contenido cargado (al cerrar sesión no debe quedar nada del usuario anterior). */
    private fun clear() {
        reconcileTask?.cancel()
        reconcileTask = null
        // Corta la carga en vuelo: su respuesta ya no debe aplicarse (y el próximo usuario
        // no debe "satisfacer" su carga con la request del anterior).
        loadTask?.second?.cancel()
        loadTask = null
        lastUpdatedAt = null
        points = 0
        data = Data()
    }

    private fun bindEvents() {
        scope.launch { bus.events<AuthChangedEvent>().collect { evaluateSession() } }

        // Cambió el saldo (canje por granos hoy; otorgamientos del backend mañana): carga
        // completa — además del saldo cambian el historial y la disponibilidad de premios.
        scope.launch {
            bus.events<BeansEarnedEvent>().collect {
                if (auth.currentUser != null) scope.launch { performLoad(LoadKind.Full) }
            }
        }

        // Compra aprobada: los granos los acredita el webhook de Mercado Pago, así que una
        // recarga inmediata suele llegar ANTES que la acreditación — reconciliamos con
        // reintentos escalonados hasta ver el saldo nuevo.
        scope.launch {
            bus.events<PurchaseCompletedEvent>().collect {
                if (auth.currentUser != null) reconcileAfterPurchase()
            }
        }

        // Un canje se concretó (vale emitido o consumido): refrescamos la cuenta. El dedupe
        // en vuelo lo coalescea con el BeansEarnedEvent del mismo canje.
        scope.launch {
            bus.events<RewardRedeemedEvent>().collect {
                if (auth.currentUser != null) scope.launch { performLoad(LoadKind.Full) }
            }
        }

        // Vuelta al foreground (un solo publisher a nivel App): refresco liviano gateado por
        // TTL, solo con contenido ya cargado (el gate y el skeleton no necesitan refresco).
        scope.launch {
            bus.events<AppDidBecomeActiveEvent>().collect {
                if (auth.currentUser != null && state is State.Loaded) refreshIfStale()
            }
        }
    }

    private fun grouped(value: Int): String {
        val sign = if (value < 0) "-" else ""
        val digits = abs(value).toString()
        val sb = StringBuilder()
        val n = digits.length
        for (i in 0 until n) {
            if (i > 0 && (n - i) % 3 == 0) sb.append('.')
            sb.append(digits[i])
        }
        return "$sign$sb"
    }

    private companion object {
        val esAR = Locale("es", "AR")
        val monthYear = SimpleDateFormat("MMMM yyyy", esAR)
        val dayMonth = SimpleDateFormat("d MMM", esAR)
    }
}

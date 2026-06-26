package com.serafino.feature.store.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.serafino.architecture.AnalyticsEvent
import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.AuthChangedEvent
import com.serafino.architecture.AuthError
import com.serafino.architecture.AuthProviding
import com.serafino.architecture.BeansEarnedEvent
import com.serafino.architecture.EventBus
import com.serafino.architecture.Interactor
import com.serafino.architecture.NoOpAnalytics
import com.serafino.architecture.events
import com.serafino.domain.entities.loyalty.LoyaltyAccount
import com.serafino.domain.entities.loyalty.LoyaltyReward
import com.serafino.domain.entities.loyalty.LoyaltyTier
import com.serafino.domain.services.LoyaltyProviding
import com.serafino.domain.services.Money
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Controla la sesión y la cuenta de fidelidad del Perfil. Sin sesión expone el gate; con sesión
 * carga la cuenta y la mapea a datos para la vista. Espeja `ProfileInteractor` de iOS.
 */
class ProfileInteractor(
    private val auth: AuthProviding,
    private val loyalty: LoyaltyProviding,
    private val bus: EventBus,
    private val analytics: AnalyticsTracking = NoOpAnalytics(),
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

    override var data by mutableStateOf(Data())
        private set
    override var state by mutableStateOf<State>(State.SignedOut)
        private set

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

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

    private fun evaluateSession(initialLoad: Boolean = false) {
        if (auth.currentUser != null) {
            if (initialLoad && state is State.Loaded) return
            load()
        } else {
            data = Data()
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

    private fun load() {
        state = State.Loading
        scope.launch { performLoad() }
    }

    private suspend fun performLoad() {
        if (auth.currentUser == null) { state = State.SignedOut; return }
        runCatching { loyalty.loadAccount() }
            .onSuccess { build(it); state = State.Loaded }
            .onFailure { error -> if (data.tiers.isEmpty()) state = State.Error(error.message ?: "No se pudo cargar.") }
    }

    private fun build(account: LoyaltyAccount) {
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

    private fun bindEvents() {
        scope.launch { bus.events<AuthChangedEvent>().collect { evaluateSession() } }
        scope.launch {
            bus.events<BeansEarnedEvent>().collect {
                if (auth.currentUser != null) performLoad()
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

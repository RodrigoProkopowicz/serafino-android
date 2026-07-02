package com.serafino.data.loyalty

import com.serafino.architecture.AuthProviding
import com.serafino.data.store.ApiException
import com.serafino.data.store.CoffeeStoreResponseMapper
import com.serafino.data.store.Http
import com.serafino.domain.BackendConfig
import com.serafino.domain.entities.loyalty.BeanTransaction
import com.serafino.domain.entities.loyalty.DirectRedeemOutcome
import com.serafino.domain.entities.loyalty.LoyaltyAccount
import com.serafino.domain.entities.loyalty.LoyaltyConfig
import com.serafino.domain.entities.loyalty.LoyaltyTier
import com.serafino.domain.entities.loyalty.LoyaltyVoucher
import com.serafino.domain.entities.loyalty.RedeemOutcome
import com.serafino.domain.services.CheckoutForm
import com.serafino.domain.services.LoyaltyProviding
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Implementación real de `LoyaltyProviding` contra el backend CoffeeStore. Lee en paralelo
 * `/loyalty/config` (público), `/loyalty/me` y `/loyalty/ledger` (con el ID token en
 * `Authorization: Bearer`). Solo transporte; el parsing vive en [LoyaltyResponseMapper]. Degrada
 * con gracia si config/ledger fallan. Espeja `BackendLoyaltyProvider` de iOS.
 *
 * Dieta de requests (el rate limit del backend es 60 lecturas/min): la escalera (`/config`)
 * se cachea en memoria por sesión, el historial (`/ledger`) se cachea por usuario, y
 * [loadAccountSummary] — el refresco frecuente de ciclo de vida — reusa ambos y pide
 * SOLO `/me` (1 GET en vez de 3). [loadAccount] sigue siendo la carga completa.
 */
class BackendLoyaltyProvider(
    private val auth: AuthProviding,
    private val fallbackTiers: List<LoyaltyTier> = LoyaltyTierCatalog.fallback,
    private val client: OkHttpClient = Http.shared,
) : LoyaltyProviding {

    /**
     * Escalera cacheada por sesión (es config pública y casi nunca cambia). Solo se guarda
     * cuando `/config` respondió bien: un fallo degrada a fallback SIN cachear (se reintenta).
     */
    @Volatile
    private var cachedTiers: List<LoyaltyTier>? = null

    /**
     * Historial cacheado del ÚLTIMO usuario ([historyUserID]): un cambio de sesión lo invalida
     * para no mostrar movimientos de otra cuenta.
     */
    @Volatile
    private var cachedHistory: List<BeanTransaction>? = null

    @Volatile
    private var historyUserID: String? = null

    override suspend fun loadPublicConfig(): LoyaltyConfig {
        val (status, body) = get("loyalty/config", token = null)
        if (status !in 200..299) throw ApiException.Server(message(status, body))
        return LoyaltyResponseMapper.config(body)
            ?: LoyaltyConfig(pointsName = "granos", amountPerPoint = 1000, tiers = fallbackTiers)
    }

    override suspend fun loadAccount(): LoyaltyAccount {
        // El uid se captura ANTES de los suspends: si la sesión cambia con la request en
        // vuelo, el historial queda cacheado a nombre del dueño real del token, no del nuevo.
        val requestUserID = auth.currentUser?.uid
        val token = auth.idToken() ?: throw ApiException.Server("Iniciá sesión para ver tus granos.")

        // Lecturas independientes en paralelo. La escalera cacheada por sesión ahorra `/config`.
        val (tiers, meStatus, meBody, ledgerStatus, ledgerBody) = coroutineScope {
            val resolvedTiers = async { resolveTiers(token) }
            val me = async { get("loyalty/me", token) }
            val ledger = async { get("loyalty/ledger", token) }
            val t = resolvedTiers.await(); val m = me.await(); val l = ledger.await()
            Five(t, m.first, m.second, l.first, l.second)
        }

        // `/me` es obligatorio.
        if (meStatus !in 200..299) throw ApiException.Server(message(meStatus, meBody))

        // Historial: secundario; si falla, cuenta sin movimientos (y sin pisar el cacheado).
        val history: List<BeanTransaction>
        if (ledgerStatus in 200..299) {
            history = LoyaltyResponseMapper.history(ledgerBody)
            cachedHistory = history
            historyUserID = requestUserID
        } else {
            history = emptyList()
        }

        return LoyaltyResponseMapper.account(meBody, tiers, history)
    }

    override suspend fun loadAccountSummary(): LoyaltyAccount {
        // Sin escalera o historial reutilizable (u otro usuario): carga completa.
        val tiers = cachedTiers
        val history = cachedHistory
        if (tiers == null || history == null || historyUserID != auth.currentUser?.uid) {
            return loadAccount()
        }
        val token = auth.idToken() ?: throw ApiException.Server("Iniciá sesión para ver tus granos.")
        val (meStatus, meBody) = get("loyalty/me", token)
        if (meStatus !in 200..299) throw ApiException.Server(message(meStatus, meBody))
        return LoyaltyResponseMapper.account(meBody, tiers, history)
    }

    /**
     * Escalera para armar la cuenta: cacheada > `/config` > fallback local. Cachea solo
     * respuestas buenas, así un fallo transitorio no congela la escalera de fallback.
     */
    private suspend fun resolveTiers(token: String): List<LoyaltyTier> {
        cachedTiers?.let { return it }
        val (configStatus, configBody) = get("loyalty/config", token)
        if (configStatus in 200..299) {
            LoyaltyResponseMapper.tiers(configBody)?.let { tiers ->
                cachedTiers = tiers
                return tiers
            }
        }
        return fallbackTiers
    }

    override suspend fun redeem(rewardId: String, idempotencyKey: String?): RedeemOutcome {
        val token = auth.idToken() ?: throw ApiException.Server("Iniciá sesión para canjear.")
        val body = JSONObject().put("rewardId", rewardId)
        idempotencyKey?.takeIf { it.isNotEmpty() }?.let { body.put("idempotencyKey", it) }
        val (status, data) = post("loyalty/redeem", body, token)
        if (status !in 200..299) throw ApiException.Server(message(status, data))
        return LoyaltyResponseMapper.redeemOutcome(data) ?: throw ApiException.InvalidResponse
    }

    override suspend fun redeemDirect(
        voucherId: String,
        productId: String?,
        grind: String?,
        contact: CheckoutForm,
    ): DirectRedeemOutcome {
        val token = auth.idToken() ?: throw ApiException.Server("Iniciá sesión para canjear.")
        val body = JSONObject().put("voucherId", voucherId)
        productId?.takeIf { it.isNotEmpty() }?.let { body.put("productId", it) }
        grind?.takeIf { it.isNotEmpty() }?.let { body.put("grind", it) }
        val payer = JSONObject().put("name", contact.name)
        if (contact.email.isNotEmpty()) payer.put("email", contact.email)
        if (contact.phone.isNotEmpty()) payer.put("phone", contact.phone)
        body.put("payer", payer)
        body.put(
            "shipping",
            JSONObject()
                .put("street", contact.street)
                .put("city", contact.city)
                .put("province", contact.province)
                .put("zip", contact.zip),
        )
        val (status, data) = post("loyalty/redeem/ship", body, token)
        if (status !in 200..299) throw ApiException.Server(message(status, data))
        return LoyaltyResponseMapper.directRedeemOutcome(data) ?: throw ApiException.InvalidResponse
    }

    override suspend fun vouchers(): List<LoyaltyVoucher> {
        val token = auth.idToken() ?: throw ApiException.Server("Iniciá sesión para ver tus canjes.")
        val (status, data) = get("loyalty/vouchers", token)
        if (status !in 200..299) throw ApiException.Server(message(status, data))
        return LoyaltyResponseMapper.vouchers(data)
    }

    // MARK: - Transporte

    private suspend fun get(path: String, token: String?): Pair<Int, String> {
        val builder = Request.Builder().url(BackendConfig.apiURL(path)).get()
        if (token != null) builder.header("Authorization", "Bearer $token")
        return Http.execute(builder.build(), client)
    }

    private suspend fun post(path: String, body: JSONObject, token: String): Pair<Int, String> {
        val request = Request.Builder()
            .url(BackendConfig.apiURL(path))
            .header("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return Http.execute(request, client)
    }

    /** Mensaje de error para un status no-2xx (429 = rate limit → copy clara). */
    private fun message(status: Int, body: String): String =
        if (status == 429) "Demasiadas solicitudes. Esperá un momento y volvé a intentar."
        else CoffeeStoreResponseMapper.errorMessage(body)

    private data class Five(
        val tiers: List<LoyaltyTier>, val meStatus: Int, val meBody: String,
        val ledgerStatus: Int, val ledgerBody: String,
    )
}

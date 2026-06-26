package com.serafino.data.loyalty

import com.serafino.architecture.AuthProviding
import com.serafino.data.store.ApiException
import com.serafino.data.store.CoffeeStoreResponseMapper
import com.serafino.data.store.Http
import com.serafino.domain.BackendConfig
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
 */
class BackendLoyaltyProvider(
    private val auth: AuthProviding,
    private val fallbackTiers: List<LoyaltyTier> = LoyaltyTierCatalog.fallback,
    private val client: OkHttpClient = Http.shared,
) : LoyaltyProviding {

    override suspend fun loadPublicConfig(): LoyaltyConfig {
        val (status, body) = get("loyalty/config", token = null)
        if (status !in 200..299) throw ApiException.Server(message(status, body))
        return LoyaltyResponseMapper.config(body)
            ?: LoyaltyConfig(pointsName = "granos", amountPerPoint = 1000, tiers = fallbackTiers)
    }

    override suspend fun loadAccount(): LoyaltyAccount {
        val token = auth.idToken() ?: throw ApiException.Server("Iniciá sesión para ver tus granos.")

        // Las tres lecturas son independientes: en paralelo.
        val (configStatus, configBody, meStatus, meBody, ledgerStatus, ledgerBody) = coroutineScope {
            val config = async { get("loyalty/config", token) }
            val me = async { get("loyalty/me", token) }
            val ledger = async { get("loyalty/ledger", token) }
            val c = config.await(); val m = me.await(); val l = ledger.await()
            Six(c.first, c.second, m.first, m.second, l.first, l.second)
        }

        // `/me` es obligatorio.
        if (meStatus !in 200..299) throw ApiException.Server(message(meStatus, meBody))

        val tiers = (if (configStatus in 200..299) LoyaltyResponseMapper.tiers(configBody) else null) ?: fallbackTiers
        val history = if (ledgerStatus in 200..299) LoyaltyResponseMapper.history(ledgerBody) else emptyList()
        return LoyaltyResponseMapper.account(meBody, tiers, history)
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

    private data class Six(
        val a: Int, val b: String, val c: Int, val d: String, val e: Int, val f: String,
    )
}

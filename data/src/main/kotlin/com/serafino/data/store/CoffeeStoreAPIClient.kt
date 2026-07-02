package com.serafino.data.store

import com.serafino.architecture.AuthProviding
import com.serafino.domain.BackendConfig
import com.serafino.domain.entities.store.Order
import com.serafino.domain.entities.store.ProductReviews
import com.serafino.domain.services.CheckoutNotice
import com.serafino.domain.services.CheckoutPreference
import com.serafino.domain.services.CheckoutRequest
import com.serafino.domain.services.CheckoutService
import com.serafino.domain.services.PromoResult
import com.serafino.domain.services.ReviewsProviding
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Cliente de la API del backend CoffeeStore (rutas `/api`): pagos, estado de pedidos, validación de
 * promos, aviso del checkout y reseñas. Solo transporte; el parsing vive en
 * [CoffeeStoreResponseMapper]. Espeja `CoffeeStoreAPIClient` de iOS.
 */
class CoffeeStoreAPIClient(
    private val auth: AuthProviding? = null,
    private val client: OkHttpClient = Http.shared,
) : CheckoutService, ReviewsProviding {

    override suspend fun createPreference(request: CheckoutRequest): CheckoutPreference {
        val itemsArray = JSONArray().apply {
            request.items.forEach { put(JSONObject().put("id", it.id).put("format", it.format).put("quantity", it.quantity)) }
        }
        val body = JSONObject()
            .put("items", itemsArray)
            .put("payer", JSONObject().put("name", request.payer.name).put("email", request.payer.email).put("phone", request.payer.phone))
            .put(
                "shipping",
                JSONObject()
                    .put("street", request.shipping.street)
                    .put("city", request.shipping.city)
                    .put("province", request.shipping.province)
                    .put("zip", request.shipping.zip),
            )
            .put("grind", request.grind)
        request.promoCode?.takeIf { it.isNotEmpty() }?.let { body.put("promoCode", it) }
        request.voucher?.let { v ->
            val obj = JSONObject().put("id", v.id)
            v.productId?.let { obj.put("productId", it) }
            body.put("voucher", obj)
        }

        // Compra logueada → manda el Bearer para acreditar granos (anónima si no hay sesión).
        val (status, data) = send("POST", "create-preference", body, authenticated = true)
        if (status !in 200..299) throw ApiException.Server(CoffeeStoreResponseMapper.errorMessage(data))
        return CoffeeStoreResponseMapper.preference(data)
    }

    override suspend fun fetchOrder(id: String): Order {
        val segment = BackendConfig.safePathSegment(id) ?: throw ApiException.InvalidResponse
        val (status, data) = send("GET", "order/$segment")
        if (status !in 200..299) throw ApiException.Server(CoffeeStoreResponseMapper.errorMessage(data))
        return CoffeeStoreResponseMapper.order(data, fallbackID = id)
    }

    override suspend fun validatePromo(code: String): PromoResult = runCatching {
        val (_, data) = send("POST", "validate-promo", JSONObject().put("code", code))
        CoffeeStoreResponseMapper.promo(data)
    }.getOrElse {
        PromoResult(valid = false, code = null, percent = null, error = "No se pudo validar el código.")
    }

    override suspend fun checkoutNotice(): CheckoutNotice? = runCatching {
        val (status, data) = send("GET", "checkout-notice")
        if (status !in 200..299) null else CoffeeStoreResponseMapper.notice(data)
    }.getOrNull()

    override suspend fun reviews(productID: String): ProductReviews {
        val segment = BackendConfig.safePathSegment(productID) ?: throw ApiException.InvalidResponse
        val (status, data) = send("GET", "reviews/$segment")
        if (status !in 200..299) throw ApiException.Server(CoffeeStoreResponseMapper.errorMessage(data))
        return CoffeeStoreResponseMapper.reviews(data)
    }

    /**
     * Devuelve `(statusCode, body)`; con `authenticated` adjunta el ID token si hay sesión;
     * sin sesión sigue anónimo. Si HAY sesión pero el token no se puede obtener (refresh
     * caído), lanza: degradar a anónimo acá crearía la compra sin acreditar los granos,
     * en silencio.
     */
    private suspend fun send(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = false,
    ): Pair<Int, String> {
        val builder = Request.Builder().url(BackendConfig.apiURL(path))
        if (body != null) {
            builder.method(method, body.toString().toRequestBody("application/json".toMediaType()))
        } else if (method == "GET") {
            builder.get()
        } else {
            builder.method(method, ByteArray(0).toRequestBody(null))
        }
        val session = auth
        if (authenticated && session?.currentUser != null) {
            val token = runCatching { session.idToken() }.getOrNull()
            if (token.isNullOrEmpty()) {
                throw ApiException.Server("No pudimos validar tu sesión. Revisá tu conexión y probá de nuevo.")
            }
            builder.header("Authorization", "Bearer $token")
        }
        return Http.execute(builder.build(), client)
    }
}

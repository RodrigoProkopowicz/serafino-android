package com.serafino.data.store

import com.serafino.domain.BackendConfig
import com.serafino.domain.entities.store.Order
import com.serafino.domain.entities.store.OrderItem
import com.serafino.domain.entities.store.OrderStatus
import com.serafino.domain.entities.store.ProductReview
import com.serafino.domain.entities.store.ProductReviews
import com.serafino.domain.services.CheckoutNotice
import com.serafino.domain.services.CheckoutPreference
import com.serafino.domain.services.PromoResult
import org.json.JSONObject
import java.util.Date

/**
 * Mapea los cuerpos JSON de la API del backend CoffeeStore a entidades de dominio. Pieza PURA: el
 * transporte lo maneja el cliente. Espeja `CoffeeStoreResponseMapper` de iOS.
 */
object CoffeeStoreResponseMapper {

    /**
     * `create-preference` → `CheckoutPreference`. Lanza si falta `orderId` o si el `init_point`
     * no es una URL de pago segura (https + host de Mercado Pago).
     */
    fun preference(body: String): CheckoutPreference {
        val json = json(body)
        val orderId = json.optStringOrNull("orderId") ?: throw ApiException.InvalidResponse
        val initString = json.optStringOrNull("init_point") ?: throw ApiException.InvalidResponse
        val initPoint = BackendConfig.paymentURL(initString) ?: throw ApiException.InvalidResponse
        return CheckoutPreference(
            orderId = orderId,
            initPoint = initPoint,
            sandboxInitPoint = json.optStringOrNull("sandbox_init_point")?.let { BackendConfig.paymentURL(it) },
        )
    }

    /** `order/:id` → `Order`. `fallbackID` se usa si el cuerpo no trae `id`. */
    fun order(body: String, fallbackID: String): Order {
        val json = json(body)
        val itemsArray = json.optJSONArray("items")
        val items = if (itemsArray != null) {
            (0 until itemsArray.length()).mapNotNull { itemsArray.optJSONObject(it) }.map { item ->
                OrderItem(
                    title = item.optString("title", ""),
                    quantity = int(item, "quantity") ?: 1,
                    unitPrice = int(item, "unit_price") ?: 0,
                )
            }
        } else emptyList()
        return Order(
            id = json.optStringOrNull("id") ?: fallbackID,
            status = OrderStatus.fromRaw(json.optString("status", "")),
            total = int(json, "total") ?: 0,
            currency = json.optString("currency", "ARS"),
            items = items,
        )
    }

    /** `reviews/:productId` → `ProductReviews`. */
    fun reviews(body: String): ProductReviews {
        val json = json(body)
        val arr = json.optJSONArray("reviews")
        val reviews = if (arr != null) {
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map { review ->
                val millis = double(review, "createdAt")
                ProductReview(
                    rating = int(review, "rating") ?: 0,
                    text = review.optString("text", ""),
                    createdAt = millis?.let { Date((it / 1000).toLong() * 1000) },
                )
            }
        } else emptyList()
        return ProductReviews(reviews = reviews, count = int(json, "count") ?: reviews.size)
    }

    /** `validate-promo` → `PromoResult`. */
    fun promo(body: String): PromoResult {
        val json = json(body)
        return if (json.optBoolean("valid", false)) {
            PromoResult(
                valid = true,
                code = json.optStringOrNull("code"),
                percent = int(json, "percent"),
                error = null,
            )
        } else {
            PromoResult(valid = false, code = null, percent = null, error = json.optStringOrNull("error"))
        }
    }

    /** `checkout-notice` → `CheckoutNotice`. */
    fun notice(body: String): CheckoutNotice = json(body).let {
        CheckoutNotice(enabled = it.optBoolean("enabled", false), text = it.optString("text", ""))
    }

    /** Extrae el mensaje de error de una respuesta de error del backend. */
    fun errorMessage(body: String): String =
        json(body).optStringOrNull("error") ?: "No se pudo completar la operación. Probá de nuevo."

    // MARK: - Lectores tolerantes

    private fun json(body: String): JSONObject =
        runCatching { JSONObject(body) }.getOrDefault(JSONObject())

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key) else null

    /** Entero tolerante: acepta números y strings (Mercado Pago manda algunos montos como string). */
    private fun int(obj: JSONObject, key: String): Int? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val v = obj.opt(key)
        return when (v) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull() ?: v.toDoubleOrNull()?.toInt()
            else -> null
        }
    }

    private fun double(obj: JSONObject, key: String): Double? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val v = obj.opt(key)
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }
    }
}

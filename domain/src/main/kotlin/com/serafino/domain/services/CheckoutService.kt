package com.serafino.domain.services

import com.serafino.domain.entities.store.Order

/**
 * Servicios de pago de la tienda (backend CoffeeStore + Mercado Pago Checkout Pro). El precio lo
 * resuelve el servidor desde Firestore: el cliente solo manda id + formato + cantidad.
 * Espeja `CheckoutService` de iOS.
 */

/** Datos para crear una preferencia de pago. */
data class CheckoutRequest(
    val items: List<Item>,
    val payer: Payer,
    val shipping: Shipping,
    /** Código promocional (opcional; el servidor lo revalida). */
    val promoCode: String?,
    /** Clave de molienda (ver `GrindOption.key`). */
    val grind: String,
    /** Vale de canje a aplicar (opcional). */
    val voucher: Voucher? = null,
) {
    data class Item(val id: String, val format: String, val quantity: Int)
    data class Payer(val name: String, val email: String, val phone: String)
    data class Shipping(val street: String, val city: String, val province: String, val zip: String)

    /** Vale de fidelidad a usar en esta compra (opcional). */
    data class Voucher(val id: String, val productId: String?)
}

/** Respuesta de `create-preference`. URLs como String (validadas por `BackendConfig`). */
data class CheckoutPreference(
    val orderId: String,
    val initPoint: String,
    val sandboxInitPoint: String?,
)

/** Resultado de validar un código promocional. */
data class PromoResult(
    val valid: Boolean,
    val code: String?,
    val percent: Int?,
    val error: String?,
)

/** Aviso editable del checkout (banner). */
data class CheckoutNotice(
    val enabled: Boolean,
    val text: String,
)

interface CheckoutService {
    /** Crea la preferencia de pago y devuelve el `init_point` de Checkout Pro. */
    suspend fun createPreference(request: CheckoutRequest): CheckoutPreference

    /** Consulta el estado de un pedido (para confirmar el pago al volver). */
    suspend fun fetchOrder(id: String): Order

    /** Previsualiza un código promocional. Nunca lanza: devuelve `valid = false` ante error. */
    suspend fun validatePromo(code: String): PromoResult

    /** Aviso del checkout. Devuelve `null` ante error (no mostrar). */
    suspend fun checkoutNotice(): CheckoutNotice?
}

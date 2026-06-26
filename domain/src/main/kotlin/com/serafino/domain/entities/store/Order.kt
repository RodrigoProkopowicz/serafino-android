package com.serafino.domain.entities.store

/**
 * Estado del pago de un pedido. El webhook de Mercado Pago lo actualiza server-side.
 * Espeja `OrderStatus` de iOS.
 */
sealed class OrderStatus {
    data object Pending : OrderStatus()
    data object Approved : OrderStatus()
    data object InProcess : OrderStatus()
    data object Rejected : OrderStatus()
    data class Unknown(val raw: String) : OrderStatus()

    /** ¿Sigue pendiente de confirmación? (vale la pena re-consultar). */
    val isPending: Boolean
        get() = when (this) {
            Pending, InProcess -> true
            Approved, Rejected, is Unknown -> false
        }

    companion object {
        /** Mapea el string crudo del backend / Mercado Pago. */
        fun fromRaw(raw: String): OrderStatus = when (raw.lowercase()) {
            "approved" -> Approved
            "pending" -> Pending
            "in_process", "inprocess" -> InProcess
            "rejected", "cancelled", "canceled", "refunded", "charged_back" -> Rejected
            else -> Unknown(raw)
        }
    }
}

data class OrderItem(
    val title: String,
    val quantity: Int,
    val unitPrice: Int,
)

data class Order(
    val id: String,
    val status: OrderStatus,
    val total: Int,
    val currency: String,
    val items: List<OrderItem>,
)

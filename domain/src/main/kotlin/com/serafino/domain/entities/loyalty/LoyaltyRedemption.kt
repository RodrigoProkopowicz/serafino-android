package com.serafino.domain.entities.loyalty

import java.util.Date

/**
 * Canje de granos: catálogo de recompensas, Café del Día y los vales que emite el backend.
 * Espeja `LoyaltyRedemption` de iOS. Los tokens de ícono (`symbolName`) los resuelve la UI.
 */

/** Tipo de recompensa, tal cual el backend (`free_product`/`merch`/`cafe_del_dia`). */
sealed class LoyaltyRewardType {
    data object FreeProduct : LoyaltyRewardType()
    data object Merch : LoyaltyRewardType()
    data object CafeDelDia : LoyaltyRewardType()
    data class Other(val raw: String) : LoyaltyRewardType()

    /** Token de ícono para la fila del canje. */
    val symbolName: String
        get() = when (this) {
            FreeProduct -> "cup"
            Merch -> "bag"
            CafeDelDia -> "calendar_clock"
            is Other -> "gift"
        }

    companion object {
        fun fromRaw(raw: String): LoyaltyRewardType = when (raw) {
            "free_product" -> FreeProduct
            "merch" -> Merch
            "cafe_del_dia" -> CafeDelDia
            else -> Other(raw)
        }
    }
}

/**
 * Una recompensa canjeable. El catálogo viene de `/config`; el estado por usuario
 * (`unlocked`/`affordable`/`canRedeem`) viene de `/me`.
 */
data class LoyaltyReward(
    val id: String,
    val type: LoyaltyRewardType,
    val cost: Int,
    /** Costo pre-formateado por el backend ("250 granos"). */
    val costLabel: String,
    val unlockTierKey: String,
    val label: String,
    /** Formato del café (solo `free_product`), ej. "250 g". */
    val format: String?,
    /** ¿El nivel del usuario lo desbloquea? (de `/me`). */
    val unlocked: Boolean,
    /** ¿Le alcanza el saldo? (de `/me`). */
    val affordable: Boolean,
    /** ¿Puede canjearlo ya? (`unlocked && affordable`). */
    val canRedeem: Boolean,
)

/**
 * Café del Día EFECTIVO de hoy (de `/me`): descuento sobre el producto que eligió el admin
 * para el día; el `pct` ya viene resuelto al nivel del usuario.
 */
data class CafeDelDia(
    val productId: String?,
    val cost: Int,
    val costLabel: String,
    val day: String,
    val dayLabel: String,
    /** % de descuento ya resuelto al nivel del usuario (5/10/15…). */
    val pct: Int,
    val maxDiscountARS: Int?,
    val affordable: Boolean,
) {
    /** Id efectivo para `POST /api/loyalty/redeem`. */
    val redeemId: String get() = rewardID

    companion object {
        /** Id reservado con el que se canjea el Café del Día por `/redeem`. */
        const val rewardID = "cafe-del-dia"
    }
}

/** Vale emitido al canjear (`/redeem`) y listado en `/vouchers`. */
data class LoyaltyVoucher(
    val id: String,
    val rewardId: String?,
    val type: LoyaltyRewardType,
    val cost: Int,
    val label: String,
    /** "active" (usable), "pending" (reservado) o "redeemed" (usado). */
    val status: String,
    val format: String?,
    val productId: String?,
    val pct: Int?,
    val maxDiscountARS: Int?,
    val createdAt: Date?,
    val expiresAt: Date?,
) {
    /** ¿El estado es activo? (sin mirar el vencimiento). */
    val isActive: Boolean get() = status == "active"

    /**
     * ¿Usable AHORA? Activo y no vencido. Espeja `decideVoucherUse` del backend
     * (`functions/lib/loyalty.js`): vence cuando `expiresAt <= now`. El endpoint `/vouchers` NO
     * filtra los vencidos, así que el cliente los descarta para no ofrecer un canje que el backend
     * va a rechazar (no mostrar un total con descuento que después no se puede cobrar).
     */
    fun isUsable(now: Date = Date()): Boolean =
        isActive && (expiresAt == null || expiresAt.after(now))
}

/** Resultado de un canje (`/redeem`): el vale emitido + el saldo resultante. */
data class RedeemOutcome(
    val voucher: LoyaltyVoucher,
    val balance: Int?,
)

/** Resultado de un canje directo (`/redeem/ship`): pedido a $0 creado + vale consumido. */
data class DirectRedeemOutcome(
    val orderId: String?,
    val voucher: LoyaltyVoucher?,
)

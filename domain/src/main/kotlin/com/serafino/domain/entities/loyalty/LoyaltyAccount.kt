package com.serafino.domain.entities.loyalty

import java.util.Date
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Categoría del movimiento, derivada del `type` del backend — para elegir el ícono y la
 * etiqueta legible. `Reversal` resta granos y NO debe leerse como una compra.
 * Espeja `BeanTransactionKind` de iOS. (`symbolName` es un token que resuelve la UI.)
 */
enum class BeanTransactionKind {
    Purchase, Bonus, Redeem, Reversal;

    val symbolName: String
        get() = when (this) {
            Purchase -> "bag"
            Bonus -> "gift"
            Redeem -> "redeem"
            Reversal -> "undo"
        }

    /** Etiqueta legible en español. */
    val label: String
        get() = when (this) {
            Purchase -> "Compra"
            Bonus -> "Bonificación"
            Redeem -> "Canje"
            Reversal -> "Reembolso"
        }
}

/** Un movimiento del historial de puntos (asiento del `ledger` del backend). */
data class BeanTransaction(
    val id: String,
    /** Tipo del asiento tal como lo manda el backend (p. ej. "earn"/"redeem"). */
    val type: String,
    /** Id de pedido COMPLETO asociado, o `null`. */
    val orderId: String?,
    /** Resumen legible del pedido que arma el backend, o `null`. */
    val orderLabel: String?,
    val date: Date,
    /** Puntos del movimiento: positivo si se ganan, negativo si se canjean. */
    val amount: Int,
    /** Categoría derivada del `type`, solo para elegir el ícono. */
    val kind: BeanTransactionKind,
) {
    val isCredit: Boolean get() = amount >= 0
}

/**
 * Cuenta de puntos del usuario, lista para presentar. Combina la verdad del backend (`/me`)
 * con la escalera de `/config` (`tiers`) y el historial de `/ledger`.
 * Espeja `LoyaltyAccount` de iOS.
 */
data class LoyaltyAccount(
    /** Cómo se llaman los puntos en la UI ("granos"). */
    val pointsName: String,
    /** Saldo de granos canjeable (el número grande). */
    val points: Int,
    /** Histórico acumulado: define el nivel y el progreso. */
    val lifetimePoints: Int,
    /** Pesos por grano (para la copy "1 grano cada $X"). */
    val amountPerPoint: Int,
    /** Clave del nivel actual (del backend). */
    val tierKey: String,
    /** Nombre del nivel actual TAL CUAL el backend. */
    val tierLabel: String,
    /** Clave del próximo nivel, o `null` si ya está en el máximo. */
    val nextTierKey: String?,
    /** Nombre del próximo nivel tal cual el backend, o `null`. */
    val nextTierLabel: String?,
    /** Puntos que faltan para el próximo nivel (del backend; 0 si máximo). */
    val pointsToNext: Int,
    /** Etiqueta pre-formateada "Te faltan N granos para X", o `null` si máximo. */
    val toNextLabel: String?,
    /** Avance de la barra hacia el próximo nivel, 0→100 (del backend). */
    val progressPercent: Int,
    /** Escalera completa de niveles (de `/config`, ya resuelta a presentación). */
    val tiers: List<LoyaltyTier>,
    /** Catálogo de canje con el estado por usuario. */
    val rewards: List<LoyaltyReward> = emptyList(),
    /** Café del Día de hoy, o `null`. */
    val cafeDelDia: CafeDelDia? = null,
    /** Movimientos recientes (recientes primero). */
    val history: List<BeanTransaction>,
) {
    /** Niveles ordenados por umbral ascendente. */
    val sortedTiers: List<LoyaltyTier> get() = tiers.sortedBy { it.threshold }

    /** Nivel actual: el de `tierKey`; si la clave no está, el más alto ya alcanzado. */
    val currentTier: LoyaltyTier
        get() {
            val sorted = sortedTiers
            return sorted.firstOrNull { it.id == tierKey }
                ?: sorted.lastOrNull { lifetimePoints >= it.threshold }
                ?: sorted.firstOrNull()
                ?: LoyaltyTier.placeholder
        }

    /** Nivel actual con el NOMBRE real del backend sobre la presentación de la escalera. */
    val displayTier: LoyaltyTier
        get() = if (tierLabel.isEmpty()) currentTier else currentTier.relabeled(tierLabel)

    /** Próximo nivel: el de `nextTierKey`; si no, el primero por encima de `lifetimePoints`. */
    val nextTier: LoyaltyTier?
        get() = nextTierKey?.let { key -> sortedTiers.firstOrNull { it.id == key } }
            ?: sortedTiers.firstOrNull { it.threshold > lifetimePoints }

    /** ¿Está en el nivel más alto? */
    val isMaxTier: Boolean get() = nextTier == null

    /** Puntos que faltan para el próximo nivel (autoridad del backend). */
    val beansToNextTier: Int get() = max(0, pointsToNext)

    /** Avance dentro del nivel actual hacia el siguiente, en `0..1` (autoridad del backend). */
    val progress: Double get() = min(100, max(0, progressPercent)) / 100.0

    /** ¿El usuario ya desbloqueó este nivel? */
    fun isUnlocked(tier: LoyaltyTier): Boolean = lifetimePoints >= tier.threshold

    companion object {
        /**
         * Construye una cuenta derivando nivel/próximo/progreso de la escalera a partir de
         * `lifetimePoints` (para datos de muestra y tests). El proveedor real toma estos valores
         * directo de `GET /api/loyalty/me`.
         */
        fun make(
            points: Int,
            lifetimePoints: Int,
            amountPerPoint: Int,
            pointsName: String = "granos",
            tiers: List<LoyaltyTier>,
            rewards: List<LoyaltyReward> = emptyList(),
            cafeDelDia: CafeDelDia? = null,
            history: List<BeanTransaction>,
        ): LoyaltyAccount {
            val sorted = tiers.sortedBy { it.threshold }
            val current = sorted.lastOrNull { lifetimePoints >= it.threshold } ?: sorted.firstOrNull()
            val next = sorted.firstOrNull { it.threshold > lifetimePoints }
            val toNext = next?.let { max(0, it.threshold - lifetimePoints) } ?: 0
            val span = (next?.threshold ?: 0) - (current?.threshold ?: 0)
            val percent = when {
                next == null -> 100
                span > 0 -> ((lifetimePoints - (current?.threshold ?: 0)).toDouble() / span * 100).roundToInt()
                else -> 0
            }
            val toNextLabel = next?.let { "Te faltan $toNext $pointsName para ${it.name}" }
            return LoyaltyAccount(
                pointsName = pointsName,
                points = points,
                lifetimePoints = lifetimePoints,
                amountPerPoint = amountPerPoint,
                tierKey = current?.id ?: "",
                tierLabel = current?.name ?: "",
                nextTierKey = next?.id,
                nextTierLabel = next?.name,
                pointsToNext = toNext,
                toNextLabel = toNextLabel,
                progressPercent = min(100, max(0, percent)),
                tiers = sorted,
                rewards = rewards,
                cafeDelDia = cafeDelDia,
                history = history,
            )
        }
    }
}

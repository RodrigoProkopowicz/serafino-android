package com.serafino.domain.entities.loyalty

/**
 * Configuración PÚBLICA del programa de puntos (`GET /api/loyalty/config`): la escalera de
 * niveles, las recompensas visibles y el Café del Día de hoy. No requiere sesión: se muestra
 * antes del login para enganchar al visitante. Espeja `LoyaltyConfig` de iOS.
 */
data class LoyaltyConfig(
    /** Nombre configurable de los puntos ("granos"). */
    val pointsName: String,
    /** Pesos por grano (para la copy "1 grano cada $X"). */
    val amountPerPoint: Int,
    /** Escalera de niveles, ya resuelta a presentación. */
    val tiers: List<LoyaltyTier>,
    /** Catálogo de recompensas visibles (sin estado por usuario). */
    val rewards: List<LoyaltyReward> = emptyList(),
    /** Café del Día de hoy (con el % por defecto), o `null`. */
    val cafeDelDia: CafeDelDia? = null,
) {
    /** Niveles ordenados por umbral ascendente (para la escalera de preview). */
    val sortedTiers: List<LoyaltyTier> get() = tiers.sortedBy { it.threshold }
}

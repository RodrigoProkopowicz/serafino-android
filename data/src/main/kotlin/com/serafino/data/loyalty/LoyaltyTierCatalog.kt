package com.serafino.data.loyalty

import com.serafino.domain.entities.loyalty.LoyaltyTier

/**
 * Capa de presentación/resolución de los niveles. La escalera la define el backend; acá quedan
 * los `perks` por `key` (que el backend no manda en fase 1), el mapeo de la clave de ícono a un
 * token semántico, y una escalera de fallback. Espeja `LoyaltyTierCatalog` de iOS.
 */
object LoyaltyTierCatalog {
    /** Beneficios por `key` de nivel (copy del cliente; fallback hasta tener datos reales). */
    val perksByKey: Map<String, List<String>> = mapOf(
        "base" to listOf(
            "Sumás 1 grano por cada compra",
            "Café del Día con 5% de descuento",
            "Canjeás granos por café gratis y merch",
        ),
        "intermedio" to listOf(
            "Café del Día con 10% de descuento",
            "Desbloqueás más recompensas del catálogo",
        ),
        "top" to listOf(
            "Café del Día con 15% de descuento",
            "Desbloqueás todo el catálogo de canje",
        ),
    )

    fun perks(key: String): List<String> = perksByKey[key] ?: emptyList()

    /** Resuelve la CLAVE de ícono del backend a un token semántico. Fallback a "leaf". */
    fun symbol(icon: String?): String = when (icon) {
        "bean" -> "leaf"
        "beans" -> "cup"
        "star" -> "star"
        "crown" -> "crown"
        "flame" -> "flame"
        else -> "leaf"
    }

    /**
     * Escalera de respaldo (si `/config` falla / dev). Espeja los defaults del backend
     * (`functions/lib/loyalty.js` DEFAULT_LOYALTY_CONFIG): nombres de marca Aficionado → Conocedor
     * → Maestro; las `key` (base/intermedio/top) no cambian (rewards.unlockTier depende de ellas).
     */
    val fallback: List<LoyaltyTier> = listOf(
        tier("base", "Aficionado", 0, "5b4231", "bean", 5, "Acá empieza tu camino en el café de especialidad. Cada compra suma granos."),
        tier("intermedio", "Conocedor", 300, "a3692c", "beans", 10, "Ya distinguís un gran café. Seguí sumando granos."),
        tier("top", "Maestro", 700, "c0833f", "star", 15, "Dominás el arte del café de especialidad. Llegaste a la cima."),
    )

    fun tier(
        key: String,
        label: String,
        min: Int,
        color: String,
        icon: String,
        cafeDelDiaPct: Int? = null,
        tagline: String,
    ): LoyaltyTier = LoyaltyTier(
        id = key,
        name = label,
        tagline = tagline,
        threshold = min,
        minLabel = "Desde $min granos",
        perks = perks(key),
        symbolName = symbol(icon),
        accentHex = color,
        cafeDelDiaPct = cafeDelDiaPct,
    )
}

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

    /** Escalera de respaldo (si `/config` falla / dev). Espeja los defaults del backend. */
    val fallback: List<LoyaltyTier> = listOf(
        tier("base", "Base", 0, "5b4231", "bean", 5, "Cada compra suma granos. Acá empieza tu camino."),
        tier("intermedio", "Intermedio", 300, "a3692c", "beans", 10, "Ya le tomaste el gusto. Seguí sumando."),
        tier("top", "Top", 700, "c0833f", "star", 15, "Llegaste a la cima del café de especialidad."),
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

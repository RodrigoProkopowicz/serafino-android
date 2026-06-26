package com.serafino.domain.entities.store

/**
 * Filtro por perfil de tueste del catálogo. Cada café cae en un bucket según su `roast`; los
 * productos sin tueste (combos) no entran en ningún bucket. Espeja `RoastFilter` de iOS.
 */
enum class RoastFilter(val rawValue: String) {
    All("todos"),
    Light("claro"),
    Medium("medio"),
    Dark("oscuro");

    val id: String get() = rawValue

    val label: String
        get() = when (this) {
            All -> "Todos"
            Light -> "Tueste claro"
            Medium -> "Medio"
            Dark -> "Oscuro"
        }

    companion object {
        /** Bucket de un café según su `roast` (espeja `ROAST_BUCKET`). `null` si no entra. */
        fun bucket(roast: String): RoastFilter? = when (roast.lowercase()) {
            "clara", "clara-media" -> Light
            "media" -> Medium
            "media-oscura", "oscura" -> Dark
            else -> null
        }
    }
}

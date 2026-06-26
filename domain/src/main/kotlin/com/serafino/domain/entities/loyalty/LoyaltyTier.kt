package com.serafino.domain.entities.loyalty

/**
 * Un nivel del programa de fidelidad "Granos". Tipo de valor PURO: no conoce UI. El color de
 * marca viaja como HEX (`accentHex`) y el ícono como token semántico (`symbolName`); la capa de
 * UI los resuelve. La escalera la define el backend (`GET /api/loyalty/config`).
 * Espeja `LoyaltyTier` de iOS.
 */
data class LoyaltyTier(
    /** Clave del nivel = la `key` del backend (p. ej. "base"). */
    val id: String,
    /** Nombre del nivel tal cual el backend (ej. "Intermedio"). */
    val name: String,
    /** Frase corta de marca del nivel (del backend `tagline`). */
    val tagline: String,
    /** Histórico (`lifetimePoints`) necesario para alcanzar este nivel — el `min` del backend. */
    val threshold: Int,
    /** Etiqueta pre-formateada del umbral, tal cual el backend ("Desde 50 granos"). */
    val minLabel: String = "",
    /** Beneficios que desbloquea (presentación del cliente). */
    val perks: List<String>,
    /** Token de ícono del nivel (resuelto desde el `icon` del backend). Lo pinta la vista. */
    val symbolName: String,
    /** Color de marca del nivel en HEX (`RRGGBB`, del backend `color`). */
    val accentHex: String,
    /** % de descuento del Café del Día que fija este nivel. `null` si el contrato no lo expone. */
    val cafeDelDiaPct: Int? = null,
) {
    /** Copia con otro nombre, conservando el resto de la presentación. */
    fun relabeled(newName: String): LoyaltyTier = copy(name = newName)

    companion object {
        /** Nivel neutro de respaldo. Solo fallback defensivo si la escalera llegara vacía. */
        val placeholder = LoyaltyTier(
            id = "placeholder",
            name = "Serafino",
            tagline = "Tu programa de granos",
            threshold = 0,
            minLabel = "",
            perks = emptyList(),
            symbolName = "leaf",
            accentHex = "C0833F",
        )
    }
}

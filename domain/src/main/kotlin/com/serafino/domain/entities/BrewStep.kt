package com.serafino.domain.entities

/**
 * Un paso de una receta. Alimenta tanto la lista del detalle como el timer en vivo.
 * Espeja `BrewStep` de iOS.
 */
data class BrewStep(
    /** Orden 1-based dentro de la receta. Estable e identificable. */
    val id: Int,
    val title: String,
    val detail: String,
    /** Duración en segundos para pasos cronometrables. `null` = paso manual. */
    val duration: Int? = null,
    /** Gramos de agua acumulados que deberían estar en el brew al terminar el paso. */
    val waterTarget: Int? = null,
    /** Texto para pasos manuales o esperas largas (p. ej. "12–18 h"). */
    val manualHint: String? = null,
    /** El "por qué" del paso, en lenguaje simple. `null` = sin explicación extra. */
    val why: String? = null,
) {
    val isTimed: Boolean get() = duration != null
}

package com.serafino.domain.entities

import kotlin.math.roundToInt

/**
 * El modelo de dominio central. Se guarda en métrico; el formateo ocurre en la capa de
 * presentación. Espeja `Recipe` de iOS.
 */
data class Recipe(
    val id: String,
    val name: String,
    val method: BrewMethod,
    val summary: String,
    val difficulty: Difficulty,
    /** Dosis de café molido, en gramos. */
    val coffeeGrams: Double,
    /** Agua total (o yield, para espresso), en gramos. */
    val waterGrams: Double,
    /** Indicación de molienda (p. ej. "Media-fina, como sal de mesa"). */
    val grind: String,
    /** Temperatura del agua, en grados Celsius. */
    val waterTempC: Int,
    /** Texto legible del tiempo total de preparación (p. ej. "2:30", "12–18 h"). */
    val brewTimeText: String,
    val steps: List<BrewStep>,
    val flavorNotes: List<String>,
    val tips: List<String>,
    /** Bienvenida en lenguaje simple para quien recién empieza. Vacío = no mostrar. */
    val beginnerIntro: String = "",
    /** Qué necesitás antes de empezar (equipo e ingredientes). */
    val equipment: List<String> = emptyList(),
    /** Palabras del café que aparecen en esta receta, explicadas. */
    val glossary: List<GlossaryTerm> = emptyList(),
) {
    /** Denominador de la ratio café:agua (1 : x). */
    val ratioDenominator: Double
        get() = if (coffeeGrams > 0) waterGrams / coffeeGrams else 0.0

    /** Ratio formateada de forma convencional (p. ej. "1:16", "1:2"). */
    val ratioText: String
        get() = "1:${ratioDenominator.roundToInt()}"

    /** ¿La receta tiene al menos un paso cronometrable? Habilita el modo timer. */
    val hasTimedSteps: Boolean
        get() = steps.any { it.isTimed }
}

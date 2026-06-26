package com.serafino.domain.entities

/**
 * Una palabra del mundo del café explicada en lenguaje simple, para que alguien que recién
 * empieza entienda los términos de una receta. Espeja `GlossaryTerm` de iOS.
 */
data class GlossaryTerm(
    val term: String,
    val definition: String,
) {
    val id: String get() = term
}

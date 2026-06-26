package com.serafino.domain.entities

/** Nivel de dificultad de una receta. Espeja `Difficulty` de iOS. */
enum class Difficulty(val rawValue: String) {
    Easy("easy"),
    Medium("medium"),
    Hard("hard");

    val id: String get() = rawValue

    val label: String
        get() = when (this) {
            Easy -> "Fácil"
            Medium -> "Media"
            Hard -> "Avanzada"
        }
}

package com.serafino.domain.entities

/**
 * Un método de preparación. Dominio puro — la presentación (colores, íconos) vive en el
 * design system. Espeja `BrewMethod` de iOS.
 */
enum class BrewMethod(val rawValue: String) {
    Espresso("espresso"),
    PourOverV60("pourOverV60"),
    Aeropress("aeropress"),
    FrenchPress("frenchPress"),
    MokaPot("mokaPot"),
    ColdBrew("coldBrew"),
    Chemex("chemex");

    val id: String get() = rawValue

    /** Nombre completo para títulos de sección y cabeceras. */
    val displayName: String
        get() = when (this) {
            Espresso -> "Espresso"
            PourOverV60 -> "Vertido · V60"
            Aeropress -> "AeroPress"
            FrenchPress -> "Prensa Francesa"
            MokaPot -> "Cafetera Moka"
            ColdBrew -> "Cold Brew"
            Chemex -> "Chemex"
        }

    /** Nombre corto para chips/etiquetas. */
    val shortName: String
        get() = when (this) {
            Espresso -> "Espresso"
            PourOverV60 -> "V60"
            Aeropress -> "AeroPress"
            FrenchPress -> "Prensa"
            MokaPot -> "Moka"
            ColdBrew -> "Cold Brew"
            Chemex -> "Chemex"
        }

    /** Orden estable de presentación en el catálogo. */
    val sortIndex: Int
        get() = when (this) {
            Espresso -> 0
            PourOverV60 -> 1
            Aeropress -> 2
            Chemex -> 3
            FrenchPress -> 4
            MokaPot -> 5
            ColdBrew -> 6
        }

    companion object {
        fun fromRaw(raw: String): BrewMethod? = entries.firstOrNull { it.rawValue == raw }
    }
}

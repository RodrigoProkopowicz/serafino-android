package com.serafino.domain.entities.store

/**
 * Molienda elegida para el pedido. La `key` viaja al backend, que es autoritativo y la mapea a
 * su etiqueta canónica. Espeja `GrindOption` de iOS.
 */
enum class GrindOption(val rawValue: String) {
    Grano("grano"),
    Gruesa("gruesa"),
    Media("media"),
    MediaFina("media-fina"),
    Fina("fina");

    val id: String get() = rawValue

    /** Clave que espera el backend. */
    val key: String get() = rawValue

    /** Etiqueta para mostrar (coincide con la canónica del backend). */
    val label: String
        get() = when (this) {
            Grano -> "En grano"
            Gruesa -> "Gruesa"
            Media -> "Media"
            MediaFina -> "Media/Fina"
            Fina -> "Fina"
        }

    /** Pista de uso para ayudar a elegir. */
    val hint: String
        get() = when (this) {
            Grano -> "Sin moler"
            Gruesa -> "Prensa francesa, cold brew"
            Media -> "V60, Chemex, filtro"
            MediaFina -> "AeroPress, moka"
            Fina -> "Espresso"
        }

    companion object {
        /** Default seguro (el café viaja en grano si no se elige). */
        val Default: GrindOption = Grano
    }
}

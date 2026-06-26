package com.serafino.domain.services

import java.util.Locale

/**
 * Disponibilidad regional de la tienda. La tienda y el programa de fidelidad solo se ofrecen en
 * Argentina; fuera de Argentina esas secciones se ocultan y queda visible solo Recetas.
 * La lógica es pura (`allowsStore`) para poder testearla. Espeja `StoreRegion` de iOS.
 */
object StoreRegion {
    /** País donde opera la tienda (ISO 3166-1 alpha-2). */
    const val countryCode = "AR"

    /**
     * ¿El código de región del usuario habilita la tienda? Case-insensitive y tolerante a `null`
     * o vacío. PURA y testeable.
     */
    fun allowsStore(regionCode: String?): Boolean {
        val code = regionCode?.trim()
        if (code.isNullOrEmpty()) return false
        return code.uppercase() == countryCode
    }

    /** Disponibilidad de la tienda según la región actual del dispositivo. */
    val isAvailableForCurrentRegion: Boolean
        get() = allowsStore(Locale.getDefault().country)
}

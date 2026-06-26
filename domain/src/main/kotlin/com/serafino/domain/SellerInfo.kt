package com.serafino.domain

/**
 * Datos del titular / vendedor, contacto y enlaces legales. ÚNICA fuente en la app. Sirve para
 * Ajustes: medios de contacto y los enlaces legales que exige la venta online en Argentina.
 * Espeja `SellerInfo` de iOS.
 */
object SellerInfo {
    // Identidad
    const val brand = "Serafino"
    const val site = "serafinocoffee.com"

    // Datos legales del titular (Ley 24.240, art. 4)
    const val legalName = "Rodrigo Nicolás Prokopowicz"
    const val cuit = "20-42196153-1"
    const val address = "Piñero 189, San Miguel, Provincia de Buenos Aires"
    const val paymentMethods = "Mercado Pago"

    // Contacto
    const val email = "serafino.coffee@gmail.com"
    const val whatsappDisplay = "11 2451-2563"
    const val instagramHandle = "@serafino.coffee"
    const val supportHours = "Lun a Vie, 9 a 18 h"

    const val emailURL = "mailto:serafino.coffee@gmail.com"
    const val whatsappURL =
        "https://wa.me/5491124512563?text=Hola%20Serafino!%20Quiero%20hacer%20una%20consulta"
    const val instagramURL = "https://www.instagram.com/serafino.coffee/"

    // Enlaces legales (venta online en Argentina)
    const val termsURL = "https://serafinocoffee.com/terminos"
    const val privacyURL = "https://serafinocoffee.com/privacidad"
    /** Botón de arrepentimiento (Resolución 424/2020). */
    const val regretURL = "https://serafinocoffee.com/arrepentimiento"
}

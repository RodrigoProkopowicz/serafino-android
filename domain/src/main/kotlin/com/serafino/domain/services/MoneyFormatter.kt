package com.serafino.domain.services

import kotlin.math.abs

/**
 * Formatea precios en pesos argentinos (espeja `formatARS` de la web: es-AR, currency ARS, sin
 * decimales, separador de miles "."). Ej.: `17000` → `"$17.000"`.
 */
object Money {
    fun ars(value: Int): String {
        val sign = if (value < 0) "-" else ""
        val digits = abs(value).toString()
        val grouped = StringBuilder()
        val n = digits.length
        for (i in 0 until n) {
            if (i > 0 && (n - i) % 3 == 0) grouped.append('.')
            grouped.append(digits[i])
        }
        return "$sign$$grouped"
    }
}

package com.serafino.domain.services

import com.serafino.domain.entities.MeasurementSystem
import com.serafino.domain.entities.TemperatureUnit
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Convierte valores métricos del dominio en strings de display honrando las preferencias del
 * usuario. Espeja `BrewUnitFormatter` de iOS.
 */
class BrewUnitFormatter(
    val system: MeasurementSystem,
    val temperatureUnit: TemperatureUnit,
) {
    /** Masa en gramos → "18 g" o "0.6 oz". */
    fun mass(grams: Double): String = when (system) {
        MeasurementSystem.Metric -> "${trimmed(grams)} g"
        MeasurementSystem.Imperial -> {
            val ounces = grams / 28.349523125
            "${trimmed(ounces, maxFractionDigits = 2)} oz"
        }
    }

    /** Temperatura en Celsius → "94°C" o "201°F". */
    fun temperature(celsius: Int): String = when (temperatureUnit) {
        TemperatureUnit.Celsius -> "$celsius°C"
        TemperatureUnit.Fahrenheit -> {
            val fahrenheit = (celsius.toDouble() * 9.0 / 5.0 + 32.0).roundToInt()
            "$fahrenheit°F"
        }
    }

    /** Segundos → "mm:ss" (p. ej. 150 → "2:30"). */
    fun duration(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return "%d:%02d".format(minutes, secs)
    }

    private fun trimmed(value: Double, maxFractionDigits: Int = 1): String {
        if (value.roundToInt().toDouble() == value) return value.toInt().toString()
        // Locale-aware (como iOS, que usa el locale del dispositivo): en es-AR el separador decimal
        // es coma ("0,63 oz"); en en, punto ("0.63 oz"). Hasta `maxFractionDigits`, sin ceros de cola.
        return NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = maxFractionDigits
            isGroupingUsed = false
            roundingMode = RoundingMode.HALF_UP
        }.format(value)
    }
}

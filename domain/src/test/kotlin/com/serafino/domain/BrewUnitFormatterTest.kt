package com.serafino.domain

import com.serafino.domain.entities.MeasurementSystem
import com.serafino.domain.entities.TemperatureUnit
import com.serafino.domain.services.BrewUnitFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

/** Espeja el formateo de unidades de `BrewUnitFormatter` de iOS. */
class BrewUnitFormatterTest {
    private val metric = BrewUnitFormatter(MeasurementSystem.Metric, TemperatureUnit.Celsius)
    private val imperial = BrewUnitFormatter(MeasurementSystem.Imperial, TemperatureUnit.Fahrenheit)

    @Test fun massMetric() {
        assertEquals("18 g", metric.mass(18.0))
        assertEquals("250 g", metric.mass(250.0))
    }

    @Test fun massImperial() {
        assertEquals("1 oz", imperial.mass(28.349523125))
        // El separador decimal es locale-aware (como iOS): coma en es, punto en en. Derivamos el
        // esperado del locale del entorno para que el test sea robusto en cualquier máquina de build.
        val dec = java.text.DecimalFormatSymbols.getInstance(java.util.Locale.getDefault()).decimalSeparator
        assertEquals("0${dec}63 oz", imperial.mass(18.0))
    }

    @Test fun temperature() {
        assertEquals("94°C", metric.temperature(94))
        assertEquals("201°F", imperial.temperature(94))
    }

    @Test fun duration() {
        assertEquals("2:30", metric.duration(150))
        assertEquals("0:00", metric.duration(0))
        assertEquals("1:05", metric.duration(65))
    }
}

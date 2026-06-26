package com.serafino.domain.entities

/** Sistema de medida para masa (café/agua). Espeja `MeasurementSystem` de iOS. */
enum class MeasurementSystem(val rawValue: String) {
    Metric("metric"),
    Imperial("imperial");

    val id: String get() = rawValue

    val label: String
        get() = when (this) {
            Metric -> "Gramos"
            Imperial -> "Onzas"
        }

    companion object {
        fun fromRaw(raw: String?): MeasurementSystem =
            entries.firstOrNull { it.rawValue == raw } ?: Metric
    }
}

/** Unidad de temperatura del agua. Espeja `TemperatureUnit` de iOS. */
enum class TemperatureUnit(val rawValue: String) {
    Celsius("celsius"),
    Fahrenheit("fahrenheit");

    val id: String get() = rawValue

    val label: String
        get() = when (this) {
            Celsius -> "Celsius"
            Fahrenheit -> "Fahrenheit"
        }

    companion object {
        fun fromRaw(raw: String?): TemperatureUnit =
            entries.firstOrNull { it.rawValue == raw } ?: Celsius
    }
}

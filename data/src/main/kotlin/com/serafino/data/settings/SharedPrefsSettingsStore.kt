package com.serafino.data.settings

import android.content.SharedPreferences
import com.serafino.architecture.EventBus
import com.serafino.architecture.SettingsChangedEvent
import com.serafino.domain.entities.MeasurementSystem
import com.serafino.domain.entities.TemperatureUnit
import com.serafino.domain.services.SettingsStoring

/**
 * Persiste preferencias en `SharedPreferences` (el análogo nativo síncrono de `UserDefaults`) y
 * publica `SettingsChangedEvent` en cada cambio. Espeja `UserDefaultsSettingsStore` de iOS.
 */
class SharedPrefsSettingsStore(
    private val prefs: SharedPreferences,
    private val bus: EventBus,
) : SettingsStoring {

    override var measurementSystem: MeasurementSystem =
        MeasurementSystem.fromRaw(prefs.getString(KEY_MEASUREMENT, null))
        set(value) {
            field = value
            prefs.edit().putString(KEY_MEASUREMENT, value.rawValue).apply()
            notifyChange()
        }

    override var temperatureUnit: TemperatureUnit =
        TemperatureUnit.fromRaw(prefs.getString(KEY_TEMPERATURE, null))
        set(value) {
            field = value
            prefs.edit().putString(KEY_TEMPERATURE, value.rawValue).apply()
            notifyChange()
        }

    override var hapticsEnabled: Boolean = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) {
            field = value
            prefs.edit().putBoolean(KEY_HAPTICS, value).apply()
            notifyChange()
        }

    private fun notifyChange() {
        bus.publish(SettingsChangedEvent())
    }

    private companion object {
        const val KEY_MEASUREMENT = "settings.measurementSystem"
        const val KEY_TEMPERATURE = "settings.temperatureUnit"
        const val KEY_HAPTICS = "settings.hapticsEnabled"
    }
}

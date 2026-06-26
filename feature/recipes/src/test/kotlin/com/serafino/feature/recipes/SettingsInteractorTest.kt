package com.serafino.feature.recipes

import com.serafino.architecture.EventBus
import com.serafino.domain.entities.MeasurementSystem
import com.serafino.domain.entities.TemperatureUnit
import com.serafino.feature.recipes.settings.SettingsInteractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Paridad con `SettingsInteractorTests` de iOS. */
class SettingsInteractorTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun make(
        system: MeasurementSystem = MeasurementSystem.Metric,
        temperature: TemperatureUnit = TemperatureUnit.Celsius,
        haptics: Boolean = true,
        storeAvailable: Boolean = false,
    ): Pair<SettingsInteractor, MockSettingsStore> {
        val bus = EventBus()
        val store = MockSettingsStore(bus, system, temperature, haptics)
        return SettingsInteractor(store, storeAvailable) to store
    }

    @Test fun loadsCurrentSettings() {
        val (i, _) = make(MeasurementSystem.Imperial, TemperatureUnit.Fahrenheit, haptics = false)
        i.handle(SettingsInteractor.Input.OnAppear)
        assertEquals(MeasurementSystem.Imperial, i.data.measurementSystem)
        assertEquals(TemperatureUnit.Fahrenheit, i.data.temperatureUnit)
        assertFalse(i.data.hapticsEnabled)
    }

    @Test fun exposesStoreAvailability() {
        val (withStore, _) = make(storeAvailable = true)
        withStore.handle(SettingsInteractor.Input.OnAppear)
        assertTrue(withStore.data.storeAvailable)
        val (without, _) = make(storeAvailable = false)
        without.handle(SettingsInteractor.Input.OnAppear)
        assertFalse(without.data.storeAvailable)
    }

    @Test fun setMeasurementWritesThrough() {
        val (i, store) = make()
        i.handle(SettingsInteractor.Input.OnAppear)
        i.handle(SettingsInteractor.Input.SetMeasurement(MeasurementSystem.Imperial))
        assertEquals(MeasurementSystem.Imperial, store.measurementSystem)
        assertEquals(MeasurementSystem.Imperial, i.data.measurementSystem)
    }

    @Test fun setTemperatureAndHapticsWriteThrough() {
        val (i, store) = make()
        i.handle(SettingsInteractor.Input.OnAppear)
        i.handle(SettingsInteractor.Input.SetTemperature(TemperatureUnit.Fahrenheit))
        i.handle(SettingsInteractor.Input.SetHaptics(false))
        assertEquals(TemperatureUnit.Fahrenheit, store.temperatureUnit)
        assertFalse(store.hapticsEnabled)
        assertEquals(TemperatureUnit.Fahrenheit, i.data.temperatureUnit)
        assertFalse(i.data.hapticsEnabled)
    }
}

package com.serafino.feature.recipes.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.serafino.architecture.DependencyContainer
import com.serafino.architecture.Interactor
import com.serafino.architecture.Presenter
import com.serafino.architecture.require
import com.serafino.domain.entities.MeasurementSystem
import com.serafino.domain.entities.TemperatureUnit
import com.serafino.domain.services.SettingsStoring
import com.serafino.domain.services.StoreRegion

/** Lógica de Ajustes: unidades, temperatura, hápticos y disponibilidad de tienda. Espeja `SettingsInteractor`. */
class SettingsInteractor(
    private val settings: SettingsStoring,
    private val storeAvailable: Boolean = StoreRegion.isAvailableForCurrentRegion,
) : Interactor<SettingsInteractor.Data, SettingsInteractor.Input, SettingsInteractor.State> {

    data class Data(
        val measurementSystem: MeasurementSystem = MeasurementSystem.Metric,
        val temperatureUnit: TemperatureUnit = TemperatureUnit.Celsius,
        val hapticsEnabled: Boolean = true,
        /** ¿Esta instalación ve la tienda (región Argentina)? Gatea contacto/legales. */
        val storeAvailable: Boolean = false,
    )

    sealed interface Input {
        data object OnAppear : Input
        data class SetMeasurement(val value: MeasurementSystem) : Input
        data class SetTemperature(val value: TemperatureUnit) : Input
        data class SetHaptics(val value: Boolean) : Input
    }

    enum class State { Loaded }

    override var data by mutableStateOf(Data())
        private set
    override var state by mutableStateOf(State.Loaded)
        private set

    override fun handle(input: Input) {
        when (input) {
            is Input.OnAppear -> data = Data(
                measurementSystem = settings.measurementSystem,
                temperatureUnit = settings.temperatureUnit,
                hapticsEnabled = settings.hapticsEnabled,
                storeAvailable = storeAvailable,
            )
            is Input.SetMeasurement -> {
                settings.measurementSystem = input.value
                data = data.copy(measurementSystem = input.value)
            }
            is Input.SetTemperature -> {
                settings.temperatureUnit = input.value
                data = data.copy(temperatureUnit = input.value)
            }
            is Input.SetHaptics -> {
                settings.hapticsEnabled = input.value
                data = data.copy(hapticsEnabled = input.value)
            }
        }
    }
}

/** Presenter de Ajustes (sin navegación). Espeja `SettingsPresenter` de iOS. */
class SettingsPresenter(
    override val interactor: SettingsInteractor,
) : Presenter<SettingsInteractor> {
    fun onAppear() = interactor.handle(SettingsInteractor.Input.OnAppear)
    fun setMeasurement(value: MeasurementSystem) = interactor.handle(SettingsInteractor.Input.SetMeasurement(value))
    fun setTemperature(value: TemperatureUnit) = interactor.handle(SettingsInteractor.Input.SetTemperature(value))
    fun setHaptics(value: Boolean) = interactor.handle(SettingsInteractor.Input.SetHaptics(value))
}

/** Ensambla el stack VIPER de Ajustes. Espeja `SettingsModule` de iOS. */
object SettingsModule {
    fun build(container: DependencyContainer): SettingsPresenter {
        val interactor = SettingsInteractor(settings = container.require<SettingsStoring>())
        return SettingsPresenter(interactor)
    }
}

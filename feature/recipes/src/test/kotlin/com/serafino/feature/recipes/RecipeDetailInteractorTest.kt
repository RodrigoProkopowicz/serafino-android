package com.serafino.feature.recipes

import com.serafino.architecture.EventBus
import com.serafino.domain.entities.MeasurementSystem
import com.serafino.domain.entities.TemperatureUnit
import com.serafino.feature.recipes.detail.RecipeDetailInteractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Paridad con `RecipeDetailInteractorTests` de iOS. */
class RecipeDetailInteractorTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun make(
        id: String = "test-espresso",
        favorites: Set<String> = emptySet(),
        system: MeasurementSystem = MeasurementSystem.Metric,
        temperature: TemperatureUnit = TemperatureUnit.Celsius,
    ): Triple<RecipeDetailInteractor, MockFavoritesStore, MockSettingsStore> {
        val bus = EventBus()
        val fav = MockFavoritesStore(bus, favorites)
        val settings = MockSettingsStore(bus, system, temperature)
        return Triple(RecipeDetailInteractor(id, MockRecipeCatalog(), fav, settings, bus), fav, settings)
    }

    private fun metric(i: RecipeDetailInteractor, id: String) = i.data.metrics.first { it.id == id }.value

    @Test fun loadsRecipeWithSixMetricsAndTimedSteps() {
        val (i, _, _) = make()
        i.handle(RecipeDetailInteractor.Input.OnAppear)
        assertEquals(RecipeDetailInteractor.State.Loaded, i.state)
        assertEquals("Test Espresso", i.data.name)
        assertEquals(6, i.data.metrics.size)
        assertEquals(2, i.data.steps.size)
        assertTrue(i.data.hasTimedSteps)
    }

    @Test fun formatsMetric() {
        val (i, _, _) = make(system = MeasurementSystem.Metric, temperature = TemperatureUnit.Celsius)
        i.handle(RecipeDetailInteractor.Input.OnAppear)
        assertEquals("18 g", metric(i, "coffee"))
        assertEquals("36 g", metric(i, "water"))
        assertEquals("1:2", metric(i, "ratio"))
        assertEquals("93°C", metric(i, "temp"))
    }

    @Test fun formatsImperialFahrenheit() {
        val (i, _, _) = make(system = MeasurementSystem.Imperial, temperature = TemperatureUnit.Fahrenheit)
        i.handle(RecipeDetailInteractor.Input.OnAppear)
        assertTrue(metric(i, "coffee").contains("oz"))
        assertEquals("199°F", metric(i, "temp")) // 93°C → 199°F
    }

    @Test fun mapsStepsDurationWaterAndManualHint() {
        val (i, _, _) = make()
        i.handle(RecipeDetailInteractor.Input.OnAppear)
        val manual = i.data.steps[0]
        assertFalse(manual.isTimed)
        assertEquals("Level", manual.manualHint)
        val timed = i.data.steps[1]
        assertTrue(timed.isTimed)
        assertEquals("0:03", timed.durationText)
        assertTrue(timed.waterText!!.contains("36"))
    }

    @Test fun notFound() {
        val (i, _, _) = make(id = "does-not-exist")
        i.handle(RecipeDetailInteractor.Input.OnAppear)
        assertEquals(RecipeDetailInteractor.State.NotFound, i.state)
    }

    @Test fun reflectsInitialFavorite() {
        val (i, _, _) = make(favorites = setOf("test-espresso"))
        i.handle(RecipeDetailInteractor.Input.OnAppear)
        assertTrue(i.data.isFavorite)
    }

    @Test fun togglesFavorite() {
        val (i, store, _) = make()
        i.handle(RecipeDetailInteractor.Input.OnAppear)
        assertFalse(i.data.isFavorite)
        i.handle(RecipeDetailInteractor.Input.ToggleFavorite)
        assertTrue(store.isFavorite("test-espresso"))
        assertTrue(i.data.isFavorite)
    }

    @Test fun reformatsOnSettingsChange() {
        val (i, _, settings) = make(system = MeasurementSystem.Metric)
        i.handle(RecipeDetailInteractor.Input.OnAppear)
        assertEquals("18 g", metric(i, "coffee"))
        settings.measurementSystem = MeasurementSystem.Imperial // publica SettingsChangedEvent
        assertTrue(metric(i, "coffee").contains("oz"))
    }
}

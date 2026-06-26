package com.serafino.feature.recipes

import com.serafino.architecture.EventBus
import com.serafino.architecture.FavoriteToggledEvent
import com.serafino.architecture.SettingsChangedEvent
import com.serafino.domain.entities.BrewMethod
import com.serafino.domain.entities.BrewStep
import com.serafino.domain.entities.Difficulty
import com.serafino.domain.entities.MeasurementSystem
import com.serafino.domain.entities.Recipe
import com.serafino.domain.entities.TemperatureUnit
import com.serafino.domain.services.FavoritesStoring
import com.serafino.domain.services.RecipeCatalogProviding
import com.serafino.domain.services.SettingsStoring
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Regla JUnit que instala un dispatcher de test como Main (los interactors crean su scope en Main). */
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}

/** Datos de muestra deterministas. Espeja `SampleRecipes` de iOS. */
object SampleRecipes {
    /** Espresso: paso 1 MANUAL, paso 2 CRONOMETRADO (3 s). Para transiciones manual→timed. */
    val espresso = Recipe(
        id = "test-espresso", name = "Test Espresso", method = BrewMethod.Espresso,
        summary = "Bright and bold.", difficulty = Difficulty.Medium,
        coffeeGrams = 18.0, waterGrams = 36.0, grind = "Fine", waterTempC = 93, brewTimeText = "0:30",
        steps = listOf(
            BrewStep(1, "Tamp", "Tamp level.", manualHint = "Level"),
            BrewStep(2, "Pull", "Pull the shot.", duration = 3, waterTarget = 36),
        ),
        flavorNotes = listOf("Caramel", "Cocoa"), tips = listOf("Aim for 1:2."),
    )

    /** V60: ambos pasos cronometrados (2 s c/u). Para una corrida completa. */
    val v60 = Recipe(
        id = "test-v60", name = "Test V60", method = BrewMethod.PourOverV60,
        summary = "Clean and floral.", difficulty = Difficulty.Easy,
        coffeeGrams = 15.0, waterGrams = 250.0, grind = "Medium", waterTempC = 94, brewTimeText = "2:30",
        steps = listOf(
            BrewStep(1, "Bloom", "Bloom.", duration = 2, waterTarget = 45),
            BrewStep(2, "Pour", "Pour.", duration = 2, waterTarget = 250),
        ),
        flavorNotes = listOf("Floral"), tips = listOf("Steady pour."),
    )

    val frenchPress = Recipe(
        id = "test-fp", name = "Test French Press", method = BrewMethod.FrenchPress,
        summary = "Full bodied.", difficulty = Difficulty.Easy,
        coffeeGrams = 30.0, waterGrams = 500.0, grind = "Coarse", waterTempC = 95, brewTimeText = "4:00",
        steps = listOf(BrewStep(1, "Steep", "Steep.", duration = 2, waterTarget = 500)),
        flavorNotes = listOf("Cocoa"), tips = emptyList(),
    )

    val all: List<Recipe> = listOf(espresso, v60, frenchPress)
}

class MockRecipeCatalog(var recipes: List<Recipe> = SampleRecipes.all) : RecipeCatalogProviding {
    override fun allRecipes(): List<Recipe> = recipes
    override fun recipe(id: String): Recipe? = recipes.firstOrNull { it.id == id }
    override fun recipes(method: BrewMethod): List<Recipe> = recipes.filter { it.method == method }
}

class MockFavoritesStore(private val bus: EventBus, initial: Set<String> = emptySet()) : FavoritesStoring {
    override var favoriteIDs: Set<String> = initial
        private set
    var toggleCount = 0
        private set

    override fun isFavorite(id: String): Boolean = favoriteIDs.contains(id)
    override fun toggle(id: String) { toggleCount++; setFavorite(id, !isFavorite(id)) }
    override fun setFavorite(id: String, value: Boolean) {
        if (value == isFavorite(id)) return
        favoriteIDs = if (value) favoriteIDs + id else favoriteIDs - id
        bus.publish(FavoriteToggledEvent(id, value))
    }
}

class MockSettingsStore(
    private val bus: EventBus,
    measurementSystem: MeasurementSystem = MeasurementSystem.Metric,
    temperatureUnit: TemperatureUnit = TemperatureUnit.Celsius,
    hapticsEnabled: Boolean = true,
) : SettingsStoring {
    override var measurementSystem: MeasurementSystem = measurementSystem
        set(value) { field = value; bus.publish(SettingsChangedEvent()) }
    override var temperatureUnit: TemperatureUnit = temperatureUnit
        set(value) { field = value; bus.publish(SettingsChangedEvent()) }
    override var hapticsEnabled: Boolean = hapticsEnabled
        set(value) { field = value; bus.publish(SettingsChangedEvent()) }
}

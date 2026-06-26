package com.serafino.feature.recipes.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.serafino.architecture.AnalyticsEvent
import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.EventBus
import com.serafino.architecture.FavoriteToggledEvent
import com.serafino.architecture.Interactor
import com.serafino.architecture.NoOpAnalytics
import com.serafino.architecture.SettingsChangedEvent
import com.serafino.architecture.events
import com.serafino.domain.entities.BrewMethod
import com.serafino.domain.entities.Difficulty
import com.serafino.domain.entities.GlossaryTerm
import com.serafino.domain.entities.Recipe
import com.serafino.domain.services.BrewUnitFormatter
import com.serafino.domain.services.FavoritesStoring
import com.serafino.domain.services.RecipeCatalogProviding
import com.serafino.domain.services.SettingsStoring
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Construye el modelo de presentación (sensible a unidades) de una receta. Espeja
 * `RecipeDetailInteractor` de iOS.
 */
class RecipeDetailInteractor(
    private val recipeID: String,
    private val catalog: RecipeCatalogProviding,
    private val favorites: FavoritesStoring,
    private val settings: SettingsStoring,
    private val bus: EventBus,
    private val analytics: AnalyticsTracking = NoOpAnalytics(),
) : Interactor<RecipeDetailInteractor.Data, RecipeDetailInteractor.Input, RecipeDetailInteractor.State> {

    /** Métrica clave. `id` identifica el ícono y el orden (coffee/water/ratio/temp/time/level). */
    data class Metric(val id: String, val value: String, val label: String)

    data class StepRow(
        val id: Int,
        val index: Int,
        val title: String,
        val detail: String,
        val durationText: String?,
        val waterText: String?,
        val manualHint: String?,
        val why: String?,
        val isTimed: Boolean,
    )

    data class Data(
        val name: String = "",
        val method: BrewMethod = BrewMethod.Espresso,
        val summary: String = "",
        val difficulty: Difficulty = Difficulty.Easy,
        val grind: String = "",
        val beginnerIntro: String = "",
        val equipment: List<String> = emptyList(),
        val glossary: List<GlossaryTerm> = emptyList(),
        val metrics: List<Metric> = emptyList(),
        val steps: List<StepRow> = emptyList(),
        val flavorNotes: List<String> = emptyList(),
        val tips: List<String> = emptyList(),
        val isFavorite: Boolean = false,
        val hasTimedSteps: Boolean = false,
    )

    sealed interface Input {
        data object OnAppear : Input
        data object ToggleFavorite : Input
    }

    enum class State { Idle, Loaded, NotFound }

    override var data by mutableStateOf(Data())
        private set
    override var state by mutableStateOf(State.Idle)
        private set

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var recipe: Recipe? = null

    init {
        bindEvents()
    }

    override fun handle(input: Input) {
        when (input) {
            is Input.OnAppear -> load()
            is Input.ToggleFavorite -> favorites.toggle(recipeID)
        }
    }

    fun dispose() = scope.cancel()

    private fun load() {
        val found = catalog.recipe(recipeID)
        if (found == null) {
            state = State.NotFound
            return
        }
        recipe = found
        rebuild()
        state = State.Loaded
        analytics.track(AnalyticsEvent.ViewItem(found.id, found.name, null))
    }

    private fun rebuild() {
        val recipe = recipe ?: return
        val formatter = BrewUnitFormatter(settings.measurementSystem, settings.temperatureUnit)

        val metrics = listOf(
            Metric("coffee", formatter.mass(recipe.coffeeGrams), "Café"),
            Metric("water", formatter.mass(recipe.waterGrams), "Agua"),
            Metric("ratio", recipe.ratioText, "Ratio"),
            Metric("temp", formatter.temperature(recipe.waterTempC), "Temp"),
            Metric("time", recipe.brewTimeText, "Tiempo"),
            Metric("level", recipe.difficulty.label, "Nivel"),
        )

        val steps = recipe.steps.mapIndexed { offset, step ->
            StepRow(
                id = step.id,
                index = offset + 1,
                title = step.title,
                detail = step.detail,
                durationText = step.duration?.let { formatter.duration(it) },
                waterText = step.waterTarget?.let { "→ " + formatter.mass(it.toDouble()) },
                manualHint = step.manualHint,
                why = step.why,
                isTimed = step.isTimed,
            )
        }

        data = data.copy(
            name = recipe.name,
            method = recipe.method,
            summary = recipe.summary,
            difficulty = recipe.difficulty,
            grind = recipe.grind,
            beginnerIntro = recipe.beginnerIntro,
            equipment = recipe.equipment,
            glossary = recipe.glossary,
            flavorNotes = recipe.flavorNotes,
            tips = recipe.tips,
            isFavorite = favorites.isFavorite(recipe.id),
            hasTimedSteps = recipe.hasTimedSteps,
            metrics = metrics,
            steps = steps,
        )
    }

    private fun bindEvents() {
        scope.launch {
            bus.events<FavoriteToggledEvent>().collect { event ->
                if (event.recipeID == recipeID) data = data.copy(isFavorite = event.isFavorite)
            }
        }
        scope.launch {
            bus.events<SettingsChangedEvent>().collect { rebuild() }
        }
    }
}

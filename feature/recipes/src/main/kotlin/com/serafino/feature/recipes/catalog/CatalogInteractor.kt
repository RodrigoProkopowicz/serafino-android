package com.serafino.feature.recipes.catalog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.serafino.architecture.AnalyticsEvent
import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.EventBus
import com.serafino.architecture.FavoriteToggledEvent
import com.serafino.architecture.Interactor
import com.serafino.architecture.NoOpAnalytics
import com.serafino.architecture.events
import com.serafino.designsystem.RecipeRowModel
import com.serafino.domain.entities.BrewMethod
import com.serafino.domain.entities.Recipe
import com.serafino.domain.services.FavoritesStoring
import com.serafino.domain.services.RecipeCatalogProviding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Lógica del catálogo de recetas: cargar, agrupar por método, buscar y mantener el estado de
 * favoritos sincronizado vía EventBus. Espeja `CatalogInteractor` de iOS.
 */
class CatalogInteractor(
    private val catalog: RecipeCatalogProviding,
    private val favorites: FavoritesStoring,
    private val bus: EventBus,
    private val analytics: AnalyticsTracking = NoOpAnalytics(),
) : Interactor<CatalogInteractor.Data, CatalogInteractor.Input, CatalogInteractor.State> {

    data class Section(val method: BrewMethod, val items: List<RecipeRowModel>) {
        val id: String get() = method.id
    }

    data class Data(
        val searchText: String = "",
        val sections: List<Section> = emptyList(),
        /** Filtro: mostrar solo recetas favoritas (Favoritos vive dentro de Recetas). */
        val showFavoritesOnly: Boolean = false,
        /** ¿Hay al menos una receta marcada como favorita? (para el estado vacío). */
        val hasFavorites: Boolean = false,
    )

    sealed interface Input {
        data object OnAppear : Input
        data class Search(val text: String) : Input
        data class ToggleFavorite(val id: String) : Input
        data object ToggleFavoritesFilter : Input
    }

    enum class State { Idle, Loading, Loaded, Empty }

    override var data by mutableStateOf(Data())
        private set
    override var state by mutableStateOf(State.Idle)
        private set

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var allRecipes: List<Recipe> = emptyList()
    private var didLoad = false

    init {
        bindEvents()
    }

    override fun handle(input: Input) {
        when (input) {
            is Input.OnAppear -> load()
            is Input.Search -> {
                data = data.copy(searchText = input.text)
                applyFilter()
            }
            is Input.ToggleFavorite -> favorites.toggle(input.id)
            is Input.ToggleFavoritesFilter -> {
                data = data.copy(showFavoritesOnly = !data.showFavoritesOnly)
                applyFilter()
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }

    private fun load() {
        if (didLoad) return
        didLoad = true
        state = State.Loading
        analytics.track(AnalyticsEvent.Screen("recipes"))
        allRecipes = catalog.allRecipes()
        applyFilter()
    }

    private fun applyFilter() {
        val query = data.searchText.trim().lowercase()
        val favs = favorites.favoriteIDs
        val searched = if (query.isEmpty()) allRecipes else allRecipes.filter { matches(it, query) }
        val matches = if (data.showFavoritesOnly) searched.filter { favs.contains(it.id) } else searched

        val sections = matches
            .groupBy { it.method }
            .toSortedMap(compareBy { it.sortIndex })
            .map { (method, recipes) -> Section(method, recipes.map { row(it, favs) }) }

        data = data.copy(sections = sections, hasFavorites = favs.isNotEmpty())
        state = if (sections.isEmpty()) State.Empty else State.Loaded
    }

    private fun matches(recipe: Recipe, query: String): Boolean =
        recipe.name.lowercase().contains(query) ||
            recipe.method.displayName.lowercase().contains(query) ||
            recipe.summary.lowercase().contains(query) ||
            recipe.flavorNotes.any { it.lowercase().contains(query) }

    private fun row(recipe: Recipe, favorites: Set<String>): RecipeRowModel = RecipeRowModel(
        id = recipe.id,
        name = recipe.name,
        method = recipe.method,
        summary = recipe.summary,
        ratioText = recipe.ratioText,
        timeText = recipe.brewTimeText,
        difficulty = recipe.difficulty,
        isFavorite = favorites.contains(recipe.id),
    )

    private fun bindEvents() {
        scope.launch {
            bus.events<FavoriteToggledEvent>().collect { event ->
                if (data.showFavoritesOnly) {
                    applyFilter()
                } else {
                    updateFavorite(event.recipeID, event.isFavorite)
                }
            }
        }
    }

    private fun updateFavorite(id: String, isFavorite: Boolean) {
        data = data.copy(
            sections = data.sections.map { section ->
                section.copy(items = section.items.map { if (it.id == id) it.copy(isFavorite = isFavorite) else it })
            },
        )
    }
}

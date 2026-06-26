package com.serafino.feature.recipes.catalog

import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.DependencyContainer
import com.serafino.architecture.EventBus
import com.serafino.architecture.Presenter
import com.serafino.architecture.Router
import com.serafino.architecture.AppRoute
import com.serafino.architecture.resolve
import com.serafino.architecture.require
import com.serafino.domain.services.FavoritesStoring
import com.serafino.domain.services.RecipeCatalogProviding

/** Presenter del Catálogo: traduce intents de la vista y pide navegación. Espeja `CatalogPresenter`. */
class CatalogPresenter(
    override val interactor: CatalogInteractor,
    private val router: Router,
) : Presenter<CatalogInteractor> {
    fun onAppear() = interactor.handle(CatalogInteractor.Input.OnAppear)
    fun didSelect(recipeID: String) = router.push(AppRoute.RecipeDetail(recipeID))
    fun toggleFavorite(recipeID: String) = interactor.handle(CatalogInteractor.Input.ToggleFavorite(recipeID))
    fun toggleFavoritesFilter() = interactor.handle(CatalogInteractor.Input.ToggleFavoritesFilter)
    fun search(text: String) = interactor.handle(CatalogInteractor.Input.Search(text))
}

/** Ensambla el stack VIPER del Catálogo desde el DI. Espeja `CatalogModule` de iOS. */
object CatalogModule {
    fun build(container: DependencyContainer, router: Router): CatalogPresenter {
        val interactor = CatalogInteractor(
            catalog = container.require<RecipeCatalogProviding>(),
            favorites = container.require<FavoritesStoring>(),
            bus = container.require<EventBus>(),
            analytics = container.resolve<AnalyticsTracking>() ?: com.serafino.architecture.NoOpAnalytics(),
        )
        return CatalogPresenter(interactor, router)
    }
}

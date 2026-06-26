package com.serafino.feature.recipes.detail

import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.AppRoute
import com.serafino.architecture.DependencyContainer
import com.serafino.architecture.EventBus
import com.serafino.architecture.NoOpAnalytics
import com.serafino.architecture.Presenter
import com.serafino.architecture.Router
import com.serafino.architecture.resolve
import com.serafino.architecture.require
import com.serafino.domain.services.FavoritesStoring
import com.serafino.domain.services.RecipeCatalogProviding
import com.serafino.domain.services.SettingsStoring

/** Presenter del detalle. Espeja `RecipeDetailPresenter` de iOS. */
class RecipeDetailPresenter(
    override val interactor: RecipeDetailInteractor,
    private val router: Router,
    private val recipeID: String,
) : Presenter<RecipeDetailInteractor> {
    fun onAppear() = interactor.handle(RecipeDetailInteractor.Input.OnAppear)
    fun toggleFavorite() = interactor.handle(RecipeDetailInteractor.Input.ToggleFavorite)
    fun startBrewing() = router.push(AppRoute.BrewTimer(recipeID))
}

/** Ensambla el stack VIPER del detalle. Espeja `RecipeDetailModule` de iOS. */
object RecipeDetailModule {
    fun build(recipeID: String, container: DependencyContainer, router: Router): RecipeDetailPresenter {
        val interactor = RecipeDetailInteractor(
            recipeID = recipeID,
            catalog = container.require<RecipeCatalogProviding>(),
            favorites = container.require<FavoritesStoring>(),
            settings = container.require<SettingsStoring>(),
            bus = container.require<EventBus>(),
            analytics = container.resolve<AnalyticsTracking>() ?: NoOpAnalytics(),
        )
        return RecipeDetailPresenter(interactor, router, recipeID)
    }
}

package com.serafino.feature.recipes.timer

import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.DependencyContainer
import com.serafino.architecture.EventBus
import com.serafino.architecture.NoOpAnalytics
import com.serafino.architecture.Presenter
import com.serafino.architecture.Router
import com.serafino.architecture.resolve
import com.serafino.architecture.require
import com.serafino.domain.services.RecipeCatalogProviding
import com.serafino.domain.services.SettingsStoring

/** Presenter del timer. Espeja `BrewTimerPresenter` de iOS. */
class BrewTimerPresenter(
    override val interactor: BrewTimerInteractor,
    private val router: Router,
) : Presenter<BrewTimerInteractor> {
    fun onAppear() = interactor.handle(BrewTimerInteractor.Input.OnAppear)
    fun onDisappear() = interactor.handle(BrewTimerInteractor.Input.OnDisappear)
    fun togglePrimary() = interactor.handle(BrewTimerInteractor.Input.TogglePrimary)
    fun nextStep() = interactor.handle(BrewTimerInteractor.Input.NextStep)
    fun previousStep() = interactor.handle(BrewTimerInteractor.Input.PreviousStep)
    fun restart() = interactor.handle(BrewTimerInteractor.Input.Restart)
    fun done() = router.pop()
}

/** Ensambla el stack VIPER del timer. Espeja `BrewTimerModule` de iOS. */
object BrewTimerModule {
    fun build(recipeID: String, container: DependencyContainer, router: Router): BrewTimerPresenter {
        val interactor = BrewTimerInteractor(
            recipeID = recipeID,
            catalog = container.require<RecipeCatalogProviding>(),
            settings = container.require<SettingsStoring>(),
            bus = container.require<EventBus>(),
            analytics = container.resolve<AnalyticsTracking>() ?: NoOpAnalytics(),
        )
        return BrewTimerPresenter(interactor, router)
    }
}

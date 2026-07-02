package com.serafino.feature.store.catalog

import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.AppRoute
import com.serafino.architecture.DependencyContainer
import com.serafino.architecture.EventBus
import com.serafino.architecture.NoOpAnalytics
import com.serafino.architecture.Presenter
import com.serafino.architecture.Router
import com.serafino.architecture.resolve
import com.serafino.architecture.require
import com.serafino.designsystem.Haptics
import com.serafino.domain.entities.store.RoastFilter
import com.serafino.domain.services.CartStoring
import com.serafino.domain.services.ProductCatalogProviding

/** Presenter del catálogo de tienda. Espeja `StoreCatalogPresenter` de iOS. */
class StoreCatalogPresenter(
    override val interactor: StoreCatalogInteractor,
    private val router: Router,
) : Presenter<StoreCatalogInteractor> {
    fun onAppear() = interactor.handle(StoreCatalogInteractor.Input.OnAppear)

    /** Mantiene el catálogo al día mientras la Tienda está visible (auto-refresco periódico). */
    suspend fun autoRefresh() = interactor.autoRefresh()

    /** Pull-to-refresh: espera la recarga para que el control nativo mantenga el spinner. */
    suspend fun refresh() = interactor.refresh()

    fun retry() = interactor.handle(StoreCatalogInteractor.Input.Retry)
    fun selectRoast(roast: RoastFilter) {
        Haptics.tap()
        interactor.handle(StoreCatalogInteractor.Input.SelectRoast(roast))
    }
    fun didSelect(productID: String) = router.push(AppRoute.ProductDetail(productID))
    fun openCart() = router.push(AppRoute.Cart)
}

/** Ensambla el stack VIPER de la tienda. Espeja `StoreCatalogModule` de iOS. */
object StoreCatalogModule {
    fun build(container: DependencyContainer, router: Router): StoreCatalogPresenter {
        val interactor = StoreCatalogInteractor(
            catalog = container.require<ProductCatalogProviding>(),
            cart = container.require<CartStoring>(),
            bus = container.require<EventBus>(),
            analytics = container.resolve<AnalyticsTracking>() ?: NoOpAnalytics(),
        )
        return StoreCatalogPresenter(interactor, router)
    }
}

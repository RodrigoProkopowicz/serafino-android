package com.serafino.feature.store.detail

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
import com.serafino.domain.services.CartStoring
import com.serafino.domain.services.ProductCatalogProviding
import com.serafino.domain.services.ReviewsProviding

/** Presenter del detalle de producto. Espeja `ProductDetailPresenter` de iOS. */
class ProductDetailPresenter(
    override val interactor: ProductDetailInteractor,
    private val router: Router,
) : Presenter<ProductDetailInteractor> {
    fun onAppear() = interactor.handle(ProductDetailInteractor.Input.OnAppear)
    fun selectFormat(weight: String) { Haptics.tap(); interactor.handle(ProductDetailInteractor.Input.SelectFormat(weight)) }
    fun incrementQuantity() { Haptics.tap(); interactor.handle(ProductDetailInteractor.Input.IncrementQuantity) }
    fun decrementQuantity() { Haptics.tap(); interactor.handle(ProductDetailInteractor.Input.DecrementQuantity) }
    fun addToCart() { Haptics.success(); interactor.handle(ProductDetailInteractor.Input.AddToCart) }
    fun retryReviews() = interactor.handle(ProductDetailInteractor.Input.LoadReviews)
    fun openCart() = router.push(AppRoute.Cart)
}

/** Ensambla el stack VIPER del detalle de producto. Espeja `ProductDetailModule` de iOS. */
object ProductDetailModule {
    fun build(productID: String, container: DependencyContainer, router: Router): ProductDetailPresenter {
        val interactor = ProductDetailInteractor(
            productID = productID,
            catalog = container.require<ProductCatalogProviding>(),
            cart = container.require<CartStoring>(),
            reviewsProvider = container.require<ReviewsProviding>(),
            bus = container.require<EventBus>(),
            analytics = container.resolve<AnalyticsTracking>() ?: NoOpAnalytics(),
        )
        return ProductDetailPresenter(interactor, router)
    }
}

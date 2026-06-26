package com.serafino.feature.store.order

import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.DependencyContainer
import com.serafino.architecture.NoOpAnalytics
import com.serafino.architecture.Presenter
import com.serafino.architecture.Router
import com.serafino.architecture.resolve
import com.serafino.architecture.require
import com.serafino.domain.services.CartStoring
import com.serafino.domain.services.CheckoutService

/** Presenter del estado del pedido. Espeja `OrderStatusPresenter` de iOS. */
class OrderStatusPresenter(
    override val interactor: OrderStatusInteractor,
    private val router: Router,
) : Presenter<OrderStatusInteractor> {
    fun onAppear() = interactor.handle(OrderStatusInteractor.Input.OnAppear)
    fun refresh() = interactor.handle(OrderStatusInteractor.Input.Refresh)
    fun continueShopping() = router.popToRoot()
}

/** Ensambla el stack VIPER del estado del pedido. Espeja `OrderStatusModule` de iOS. */
object OrderStatusModule {
    fun build(orderID: String, container: DependencyContainer, router: Router): OrderStatusPresenter {
        val interactor = OrderStatusInteractor(
            orderID = orderID,
            checkout = container.require<CheckoutService>(),
            cart = container.require<CartStoring>(),
            analytics = container.resolve<AnalyticsTracking>() ?: NoOpAnalytics(),
        )
        return OrderStatusPresenter(interactor, router)
    }
}

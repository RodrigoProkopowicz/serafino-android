package com.serafino.feature.store.checkout

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
import com.serafino.domain.entities.store.GrindOption
import com.serafino.domain.services.CartStoring
import com.serafino.domain.services.CheckoutField
import com.serafino.domain.services.CheckoutProfileStoring
import com.serafino.domain.services.CheckoutService
import com.serafino.domain.services.LoyaltyProviding
import com.serafino.domain.services.ProductCatalogProviding

/** Presenter del checkout. Espeja `CheckoutPresenter` de iOS. */
class CheckoutPresenter(
    override val interactor: CheckoutInteractor,
    private val router: Router,
) : Presenter<CheckoutInteractor> {
    fun onAppear() = interactor.handle(CheckoutInteractor.Input.OnAppear)
    fun updateField(field: CheckoutField, value: String) = interactor.handle(CheckoutInteractor.Input.UpdateField(field, value))
    fun selectGrind(grind: GrindOption) = interactor.handle(CheckoutInteractor.Input.SelectGrind(grind))
    fun updatePromo(code: String) = interactor.handle(CheckoutInteractor.Input.UpdatePromoCode(code))
    fun validatePromo() = interactor.handle(CheckoutInteractor.Input.ValidatePromo)
    fun selectVoucher(id: String?) { Haptics.tap(); interactor.handle(CheckoutInteractor.Input.SelectVoucher(id)) }
    fun selectFreeCoffee(id: String) = interactor.handle(CheckoutInteractor.Input.SelectFreeCoffee(id))
    fun pay() { Haptics.tap(); interactor.handle(CheckoutInteractor.Input.Pay) }
    fun clearPayment() = interactor.handle(CheckoutInteractor.Input.ClearPayment)
    fun didReturnFromPayment() {
        interactor.data.lastOrderId?.let { router.push(AppRoute.OrderStatus(it)) }
    }
}

/** Ensambla el stack VIPER del checkout. Espeja `CheckoutModule` de iOS. */
object CheckoutModule {
    fun build(container: DependencyContainer, router: Router): CheckoutPresenter {
        val interactor = CheckoutInteractor(
            cart = container.require<CartStoring>(),
            checkout = container.require<CheckoutService>(),
            analytics = container.resolve<AnalyticsTracking>() ?: NoOpAnalytics(),
            loyalty = container.resolve<LoyaltyProviding>(),
            catalog = container.resolve<ProductCatalogProviding>(),
            profile = container.resolve<CheckoutProfileStoring>(),
            bus = container.resolve<EventBus>(),
        )
        return CheckoutPresenter(interactor, router)
    }
}

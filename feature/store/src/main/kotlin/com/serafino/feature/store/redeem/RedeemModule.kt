package com.serafino.feature.store.redeem

import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.DependencyContainer
import com.serafino.architecture.EventBus
import com.serafino.architecture.NoOpAnalytics
import com.serafino.architecture.Presenter
import com.serafino.architecture.Router
import com.serafino.architecture.resolve
import com.serafino.architecture.require
import com.serafino.domain.entities.store.GrindOption
import com.serafino.domain.services.CheckoutField
import com.serafino.domain.services.CheckoutProfileStoring
import com.serafino.domain.services.LoyaltyProviding
import com.serafino.domain.services.ProductCatalogProviding

/** Presenter de "Canjear granos". Espeja `RedeemPresenter` de iOS. */
class RedeemPresenter(
    override val interactor: RedeemInteractor,
) : Presenter<RedeemInteractor> {
    fun onAppear() = interactor.handle(RedeemInteractor.Input.OnAppear)
    fun retry() = interactor.handle(RedeemInteractor.Input.Retry)
    fun redeem(id: String) = interactor.handle(RedeemInteractor.Input.Redeem(id))
    fun selectShipCoffee(voucherID: String, productID: String) = interactor.handle(RedeemInteractor.Input.SelectShipCoffee(voucherID, productID))
    fun redeemDirect(id: String) = interactor.handle(RedeemInteractor.Input.RedeemDirect(id))
    fun updateShippingField(field: CheckoutField, value: String) = interactor.handle(RedeemInteractor.Input.UpdateShippingField(field, value))
    fun selectShippingGrind(grind: GrindOption) = interactor.handle(RedeemInteractor.Input.SelectShippingGrind(grind))
    fun confirmShipping() = interactor.handle(RedeemInteractor.Input.ConfirmShipping)
    fun cancelShipping() = interactor.handle(RedeemInteractor.Input.CancelShipping)
    fun clearResult() = interactor.handle(RedeemInteractor.Input.ClearResult)
    fun clearError() = interactor.handle(RedeemInteractor.Input.ClearError)
}

/** Ensambla el stack VIPER de "Canjear granos". Espeja `RedeemModule` de iOS. */
object RedeemModule {
    fun build(container: DependencyContainer): RedeemPresenter {
        val interactor = RedeemInteractor(
            loyalty = container.require<LoyaltyProviding>(),
            bus = container.require<EventBus>(),
            analytics = container.resolve<AnalyticsTracking>() ?: NoOpAnalytics(),
            catalog = container.resolve<ProductCatalogProviding>(),
            profile = container.resolve<CheckoutProfileStoring>(),
        )
        return RedeemPresenter(interactor)
    }
}

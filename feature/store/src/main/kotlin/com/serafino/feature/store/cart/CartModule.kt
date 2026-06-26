package com.serafino.feature.store.cart

import com.serafino.architecture.AppRoute
import com.serafino.architecture.DependencyContainer
import com.serafino.architecture.EventBus
import com.serafino.architecture.Presenter
import com.serafino.architecture.Router
import com.serafino.architecture.require
import com.serafino.designsystem.Haptics
import com.serafino.domain.services.CartStoring

/** Presenter del carrito. Espeja `CartPresenter` de iOS. */
class CartPresenter(
    override val interactor: CartInteractor,
    private val router: Router,
) : Presenter<CartInteractor> {
    fun onAppear() = interactor.handle(CartInteractor.Input.OnAppear)
    fun increment(id: String) { Haptics.tap(); interactor.handle(CartInteractor.Input.Increment(id)) }
    fun decrement(id: String) { Haptics.tap(); interactor.handle(CartInteractor.Input.Decrement(id)) }
    fun remove(id: String) = interactor.handle(CartInteractor.Input.Remove(id))
    fun checkout() = router.push(AppRoute.Checkout)
    fun explore() = router.popToRoot()
}

/** Ensambla el stack VIPER del carrito. Espeja `CartModule` de iOS. */
object CartModule {
    fun build(container: DependencyContainer, router: Router): CartPresenter {
        val interactor = CartInteractor(
            cart = container.require<CartStoring>(),
            bus = container.require<EventBus>(),
        )
        return CartPresenter(interactor, router)
    }
}

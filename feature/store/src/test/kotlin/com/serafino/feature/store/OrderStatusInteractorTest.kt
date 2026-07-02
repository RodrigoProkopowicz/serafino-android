package com.serafino.feature.store

import com.serafino.architecture.EventBus
import com.serafino.domain.entities.store.Order
import com.serafino.domain.entities.store.OrderStatus
import com.serafino.feature.store.order.OrderStatusInteractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OrderStatusInteractorTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun order(status: OrderStatus) = Order("o1", status, 17000, "ARS", emptyList())

    private fun make(checkout: MockCheckoutService): Triple<OrderStatusInteractor, MockCartStore, EventBus> {
        val bus = EventBus()
        val cart = MockCartStore(bus, listOf(cartLine(SampleProducts.aurora)))
        // maxAttempts = 1: sin reintentos ni delays (determinista en tests).
        return Triple(OrderStatusInteractor("o1", checkout, cart, bus, maxAttempts = 1), cart, bus)
    }

    /** Un pago aprobado confirma, vacía el carrito y publica PurchaseCompletedEvent. */
    @Test fun approvedClearsCart() {
        val checkout = MockCheckoutService().apply { orderQueue = mutableListOf(order(OrderStatus.Approved)) }
        val (interactor, cart, bus) = make(checkout)
        val purchases = eventProbe<com.serafino.architecture.PurchaseCompletedEvent>(bus)
        interactor.handle(OrderStatusInteractor.Input.OnAppear)
        assertEquals(OrderStatusInteractor.State.Loaded, interactor.state)
        assertEquals(OrderStatus.Approved, interactor.data.status)
        assertEquals(1, cart.clearCount)
        assertEquals("$17.000", interactor.data.totalText)
        // El Perfil escucha este evento para reconciliar los granos contra el webhook.
        assertEquals(listOf("o1"), purchases.events.map { it.orderID })
    }

    /** Un pago pendiente no vacía el carrito ni publica la compra. */
    @Test fun pendingDoesNotClearCart() {
        val checkout = MockCheckoutService().apply { orderQueue = mutableListOf(order(OrderStatus.Pending)) }
        val (interactor, cart, bus) = make(checkout)
        val purchases = eventProbe<com.serafino.architecture.PurchaseCompletedEvent>(bus)
        interactor.handle(OrderStatusInteractor.Input.OnAppear)
        assertEquals(OrderStatusInteractor.State.Loaded, interactor.state)
        assertTrue(interactor.data.status.isPending)
        assertEquals(0, cart.clearCount)
        assertTrue(purchases.events.isEmpty())
    }

    /** Pago rechazado: deja de reintentar y NO vacía el carrito (ruta de dinero asimétrica). */
    @Test fun rejectedDoesNotClearCart() {
        val checkout = MockCheckoutService().apply { orderQueue = mutableListOf(order(OrderStatus.Rejected)) }
        val (interactor, cart, bus) = make(checkout)
        val purchases = eventProbe<com.serafino.architecture.PurchaseCompletedEvent>(bus)
        interactor.handle(OrderStatusInteractor.Input.OnAppear)
        assertEquals(OrderStatusInteractor.State.Loaded, interactor.state)
        assertEquals(OrderStatus.Rejected, interactor.data.status)
        assertFalse(interactor.data.status.isPending)
        assertEquals(0, cart.clearCount)
        assertFalse(interactor.data.isPolling)
        assertTrue(purchases.events.isEmpty())   // el Perfil no reconcilia granos que no hubo
    }

    @Test fun failsWhenOrderUnavailable() {
        val checkout = MockCheckoutService() // orderQueue vacío → fetchOrder lanza
        val (interactor, cart, _) = make(checkout)
        interactor.handle(OrderStatusInteractor.Input.OnAppear)
        assertEquals(OrderStatusInteractor.State.Failed, interactor.state)
        assertEquals(0, cart.clearCount)
    }

    /** Reintenta hasta que el pago pasa de pendiente a aprobado, y limpia el carrito una sola vez. */
    @Test fun transitionsPendingToApproved() {
        val checkout = MockCheckoutService().apply {
            orderQueue = mutableListOf(order(OrderStatus.Pending), order(OrderStatus.Approved))
        }
        val bus = EventBus()
        val cart = MockCartStore(bus, listOf(cartLine(SampleProducts.aurora)))
        // Varios intentos, sin delay real: el primer fetch da pendiente, el segundo aprobado.
        val interactor = OrderStatusInteractor("o1", checkout, cart, bus, maxAttempts = 4, retryDelayMillis = 0)
        interactor.handle(OrderStatusInteractor.Input.OnAppear)
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(OrderStatusInteractor.State.Loaded, interactor.state)
        assertEquals(OrderStatus.Approved, interactor.data.status)
        assertEquals(1, cart.clearCount)
        assertTrue(checkout.fetchCount >= 2)
    }
}

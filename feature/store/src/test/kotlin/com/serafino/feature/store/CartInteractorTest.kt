package com.serafino.feature.store

import com.serafino.architecture.EventBus
import com.serafino.feature.store.cart.CartInteractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CartInteractorTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun make(lines: List<com.serafino.domain.entities.store.CartLine> = emptyList()): Pair<CartInteractor, MockCartStore> {
        val bus = EventBus()
        val cart = MockCartStore(bus, lines)
        return CartInteractor(cart, bus) to cart
    }

    @Test fun reflectsCartLines() {
        val (interactor, _) = make(listOf(cartLine(SampleProducts.aurora, 2)))
        interactor.handle(CartInteractor.Input.OnAppear)
        assertEquals(CartInteractor.State.Loaded, interactor.state)
        assertEquals(1, interactor.data.lines.size)
        assertEquals(2, interactor.data.lines.first().quantity)
        assertEquals(2, interactor.data.count)
    }

    @Test fun emptyState() {
        val (interactor, _) = make()
        interactor.handle(CartInteractor.Input.OnAppear)
        assertEquals(CartInteractor.State.Empty, interactor.state)
    }

    @Test fun mutations() {
        val line = cartLine(SampleProducts.aurora, 1)
        val (interactor, cart) = make(listOf(line))
        interactor.handle(CartInteractor.Input.Increment(line.id))
        assertEquals(2, cart.lines.first().quantity)
        interactor.handle(CartInteractor.Input.Decrement(line.id))
        assertEquals(1, cart.lines.first().quantity)
        interactor.handle(CartInteractor.Input.Remove(line.id))
        assertTrue(cart.lines.isEmpty())
        assertEquals(CartInteractor.State.Empty, interactor.state)
    }

    @Test fun decrementFromOneRemoves() {
        val line = cartLine(SampleProducts.aurora, 1)
        val (interactor, cart) = make(listOf(line))
        interactor.handle(CartInteractor.Input.Decrement(line.id))
        assertTrue(cart.lines.isEmpty())
    }

    // Incrementar no supera el tope del backend (MAX_ITEM_QUANTITY=99).
    @Test fun incrementCapsAtMax() {
        val line = cartLine(SampleProducts.aurora, 99)
        val (interactor, cart) = make(listOf(line))
        interactor.handle(CartInteractor.Input.Increment(line.id))
        assertEquals(99, cart.lines.first().quantity)
    }
}

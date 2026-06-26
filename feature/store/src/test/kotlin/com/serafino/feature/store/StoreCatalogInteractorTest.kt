package com.serafino.feature.store

import com.serafino.architecture.EventBus
import com.serafino.domain.entities.store.RoastFilter
import com.serafino.feature.store.catalog.StoreCatalogInteractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Paridad con `StoreCatalogInteractorTests` de iOS. */
class StoreCatalogInteractorTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun make(catalog: MockProductCatalog = MockProductCatalog()): Pair<StoreCatalogInteractor, MockCartStore> {
        val bus = EventBus()
        val cart = MockCartStore(bus)
        return StoreCatalogInteractor(catalog, cart, bus) to cart
    }

    @Test fun loadsAndExcludesHidden() {
        val (i, _) = make()
        i.handle(StoreCatalogInteractor.Input.OnAppear)
        assertEquals(StoreCatalogInteractor.State.Loaded, i.state)
        assertEquals("serafino-casa", i.data.featured?.id)
        val ids = i.data.products.map { it.id }
        assertTrue(ids.contains("aurora"))
        assertTrue(ids.contains("plain"))
        assertFalse(ids.contains("oculto"))        // hidden
        assertFalse(ids.contains("serafino-casa")) // featured va al hero
    }

    @Test fun cardShowsEffectivePromoPrice() {
        val (i, _) = make()
        i.handle(StoreCatalogInteractor.Input.OnAppear)
        val aurora = i.data.products.first { it.id == "aurora" }
        assertEquals(17000, aurora.pricing.price)
        assertTrue(aurora.pricing.active)
    }

    @Test fun errorState() {
        val (i, _) = make(MockProductCatalog(error = RuntimeException("sin red")))
        i.handle(StoreCatalogInteractor.Input.OnAppear)
        assertTrue(i.state is StoreCatalogInteractor.State.Error)
    }

    @Test fun cartBadgeUpdatesFromEvent() {
        val bus = EventBus()
        val cart = MockCartStore(bus)
        val i = StoreCatalogInteractor(MockProductCatalog(), cart, bus)
        i.handle(StoreCatalogInteractor.Input.OnAppear)
        cart.add(cartLine(SampleProducts.aurora, 2))
        assertEquals(2, i.data.cartCount)
    }

    @Test fun roastFilterOffersBucketsAndFiltersGrid() {
        val (i, _) = make()
        i.handle(StoreCatalogInteractor.Input.OnAppear)
        // rest: aurora(media), plain(media), notturno(media-oscura) → buckets {Medium, Dark}.
        assertEquals(listOf(RoastFilter.All, RoastFilter.Medium, RoastFilter.Dark), i.data.roastFilters)

        i.handle(StoreCatalogInteractor.Input.SelectRoast(RoastFilter.Dark))
        assertEquals(listOf("notturno"), i.data.products.map { it.id })

        i.handle(StoreCatalogInteractor.Input.SelectRoast(RoastFilter.Medium))
        assertEquals(setOf("aurora", "plain"), i.data.products.map { it.id }.toSet())

        i.handle(StoreCatalogInteractor.Input.SelectRoast(RoastFilter.All))
        assertEquals(3, i.data.products.size)
    }
}

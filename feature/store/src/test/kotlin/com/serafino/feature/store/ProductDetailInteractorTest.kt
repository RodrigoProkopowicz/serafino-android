package com.serafino.feature.store

import com.serafino.architecture.EventBus
import com.serafino.domain.entities.store.ProductReview
import com.serafino.domain.entities.store.ProductReviews
import com.serafino.feature.store.detail.ProductDetailInteractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Paridad con `ProductDetailInteractorTests` de iOS. */
class ProductDetailInteractorTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun make(productID: String = "aurora", reviews: MockReviewsProvider = MockReviewsProvider()): Triple<ProductDetailInteractor, MockCartStore, EventBus> {
        val bus = EventBus()
        val cart = MockCartStore(bus)
        val interactor = ProductDetailInteractor(productID, MockProductCatalog(), cart, reviews, bus)
        return Triple(interactor, cart, bus)
    }

    @Test fun loadsAndSelectsStandard() {
        val (i, _, _) = make()
        i.handle(ProductDetailInteractor.Input.OnAppear)
        assertEquals(ProductDetailInteractor.State.Loaded, i.state)
        assertEquals(3, i.data.formats.size)
        assertEquals("250 g", i.data.selectedWeight)
        assertEquals(17000, i.data.selectedPricing.price)
    }

    @Test fun selectFormatUpdatesPrice() {
        val (i, _, _) = make()
        i.handle(ProductDetailInteractor.Input.OnAppear)
        i.handle(ProductDetailInteractor.Input.SelectFormat("125 g"))
        assertEquals(10200, i.data.selectedPricing.price)
    }

    @Test fun addToCartUsesSelectedFormat() {
        val (i, cart, _) = make()
        i.handle(ProductDetailInteractor.Input.OnAppear)
        i.handle(ProductDetailInteractor.Input.SelectFormat("500 g"))
        i.handle(ProductDetailInteractor.Input.AddToCart)
        assertEquals(1, cart.lines.size)
        assertEquals("500 g", cart.lines.first().formatWeight)
        assertEquals(31450, cart.lines.first().unitPrice)
    }

    @Test fun loadsReviews() {
        val reviews = MockReviewsProvider(ProductReviews(listOf(ProductReview(5, "Riquísimo", null)), 1))
        val (i, _, _) = make(reviews = reviews)
        i.handle(ProductDetailInteractor.Input.OnAppear)
        assertEquals(ProductDetailInteractor.ReviewsState.Loaded, i.data.reviewsState)
        assertEquals(1, i.data.reviewCount)
        assertEquals(5.0, i.data.averageRating!!, 0.0001)
    }

    @Test fun parsesIntensity() {
        val (i, _, _) = make()
        i.handle(ProductDetailInteractor.Input.OnAppear)
        assertEquals(4.0, i.data.intensity!!, 0.0001)
    }

    @Test fun addsSelectedQuantityThenResets() {
        val (i, cart, _) = make()
        i.handle(ProductDetailInteractor.Input.OnAppear)
        i.handle(ProductDetailInteractor.Input.IncrementQuantity) // 2
        i.handle(ProductDetailInteractor.Input.IncrementQuantity) // 3
        i.handle(ProductDetailInteractor.Input.DecrementQuantity) // 2
        i.handle(ProductDetailInteractor.Input.AddToCart)
        assertEquals(2, cart.lines.first().quantity)
        assertEquals(1, i.data.quantity)
    }

    @Test fun quantityFloorIsOne() {
        val (i, _, _) = make()
        i.handle(ProductDetailInteractor.Input.OnAppear)
        i.handle(ProductDetailInteractor.Input.DecrementQuantity)
        i.handle(ProductDetailInteractor.Input.DecrementQuantity)
        assertEquals(1, i.data.quantity)
    }

    // El tope espeja MAX_ITEM_QUANTITY=99 del backend (ARQ-1: total mostrado nunca > cobrado).
    @Test fun quantityCapsAtMax() {
        val (i, cart, _) = make()
        i.handle(ProductDetailInteractor.Input.OnAppear)
        repeat(200) { i.handle(ProductDetailInteractor.Input.IncrementQuantity) }
        assertEquals(99, i.data.quantity)
        i.handle(ProductDetailInteractor.Input.AddToCart)
        assertEquals(99, cart.lines.first().quantity)
    }

    @Test fun notFound() {
        val (i, _, _) = make(productID = "no-existe")
        i.handle(ProductDetailInteractor.Input.OnAppear)
        assertEquals(ProductDetailInteractor.State.NotFound, i.state)
    }
}

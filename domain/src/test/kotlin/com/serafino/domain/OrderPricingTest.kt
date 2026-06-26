package com.serafino.domain

import com.serafino.domain.services.OrderPricing
import org.junit.Assert.assertEquals
import org.junit.Test

/** Espeja `OrderPricingTests` de iOS: un único descuento por producto. */
class OrderPricingTest {
    private fun line(id: String, unit: Int, qty: Int = 1) = OrderPricing.Line(id, unit, qty)

    @Test fun noDiscounts() {
        val r = OrderPricing.preview(listOf(line("a", 10000, 2)), null, null)
        assertEquals(20000, r.subtotal); assertEquals(0, r.discount); assertEquals(20000, r.total)
    }

    @Test fun promoAppliesToAll() {
        val r = OrderPricing.preview(listOf(line("a", 10000), line("b", 5000)), 10, null)
        assertEquals(9000 + 4500, r.total); assertEquals(1500, r.discount)
    }

    @Test fun cafeDelDiaExcludedFromPromo() {
        val r = OrderPricing.preview(
            listOf(line("cafe-dia", 10000), line("otro", 5000)),
            10,
            OrderPricing.CafeDelDia("cafe-dia", 15, null),
        )
        assertEquals(15000, r.subtotal)
        assertEquals(8500 + 4500, r.total)
        assertEquals(1500 + 500, r.discount)
    }

    @Test fun cafeDelDiaOnlyProductPromoNoOp() {
        val r = OrderPricing.preview(listOf(line("cafe-dia", 10000)), 20, OrderPricing.CafeDelDia("cafe-dia", 10, null))
        assertEquals(9000, r.total); assertEquals(1000, r.discount)
    }

    @Test fun cafeDelDiaRespectsCap() {
        val r = OrderPricing.preview(listOf(line("cafe-dia", 10000)), null, OrderPricing.CafeDelDia("cafe-dia", 50, 3000))
        assertEquals(3000, r.discount); assertEquals(7000, r.total)
    }

    @Test fun cafeDelDiaPerUnitFloor() {
        val r = OrderPricing.preview(listOf(line("x", 3333, 3)), null, OrderPricing.CafeDelDia("x", 10, null))
        assertEquals(8997, r.total); assertEquals(1002, r.discount)
    }

    @Test fun cafeDelDiaNotInCart() {
        val r = OrderPricing.preview(listOf(line("a", 10000), line("b", 5000)), 10, OrderPricing.CafeDelDia("ausente", 15, null))
        assertEquals(9000 + 4500, r.total); assertEquals(1500, r.discount)
    }
}

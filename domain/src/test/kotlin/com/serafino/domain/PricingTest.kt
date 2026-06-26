package com.serafino.domain

import com.serafino.domain.entities.store.Ficha
import com.serafino.domain.entities.store.Pricing
import com.serafino.domain.entities.store.Product
import com.serafino.domain.entities.store.ProductFormat
import com.serafino.domain.entities.store.hasFicha
import com.serafino.domain.entities.store.isCombo
import com.serafino.domain.entities.store.pricing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Espeja `PricingTests` de iOS (mismas reglas de precio efectivo que el backend). */
class PricingTest {

    private val aurora = Product(
        id = "aurora", name = "Aurora", image = "/img/products/aurora.jpg", origin = "Colombia",
        roast = "media", weight = "250 g", price = 20000, promoActive = true, promoPrice = 17000,
        accent = listOf("C0833F", "6F4A2F"), notes = listOf("Caramelo"), badge = null,
        featured = true, hidden = false,
        formats = listOf(
            ProductFormat("Degustación", "125 g", 12000, 10200),
            ProductFormat("Estándar", "250 g", 20000, 17000),
        ),
        description = "Intensidad 5/10",
        ficha = Ficha("Huila", "1800 msnm", "Caturra", "Lavado", "Finca", "2025", "Una historia"),
        comboItems = null,
    )

    private val plain = Product(
        id = "plain", name = "Plain", image = "", origin = "Brasil", roast = "oscura", weight = "250 g",
        price = 18000, promoActive = false, promoPrice = null, accent = emptyList(), notes = emptyList(),
        badge = null, featured = false, hidden = false, formats = emptyList(), description = "",
        ficha = null, comboItems = null,
    )

    @Test fun effectivePriceWithPromo() {
        val p = aurora.pricing
        assertTrue(p.active); assertEquals(20000, p.original); assertEquals(17000, p.price); assertEquals(15, p.discountPct)
    }

    @Test fun formatPricingPicksFormat() {
        val p = aurora.pricing("125 g")
        assertEquals(10200, p.price); assertEquals(12000, p.original); assertEquals(15, p.discountPct)
    }

    @Test fun formatPricingFallsBack() {
        assertEquals(17000, aurora.pricing("999 g").price)
    }

    @Test fun noPromoUsesListPrice() {
        val p = plain.pricing
        assertFalse(p.active); assertEquals(18000, p.price); assertEquals(0, p.discountPct)
    }

    @Test fun applyPercentMatchesBackend() {
        assertEquals(15300, Pricing.applyPercent(17000, 10))
        assertEquals(15300, Pricing.applyPercent(18000, 15))
    }

    @Test fun fichaAndCombo() {
        assertTrue(aurora.hasFicha); assertFalse(plain.hasFicha); assertFalse(aurora.isCombo)
    }
}

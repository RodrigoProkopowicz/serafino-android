package com.serafino.data

import com.serafino.data.loyalty.LoyaltyTierCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * La escalera de respaldo debe espejar los defaults de marca del backend
 * (`functions/lib/loyalty.js` DEFAULT_LOYALTY_CONFIG): Aficionado → Conocedor → Maestro, con las
 * `key` base/intermedio/top intactas (rewards.unlockTier depende de ellas).
 */
class LoyaltyTierCatalogTest {

    @Test fun fallbackMirrorsBackendBrandLadder() {
        val tiers = LoyaltyTierCatalog.fallback
        assertEquals(listOf("base", "intermedio", "top"), tiers.map { it.id })
        assertEquals(listOf("Aficionado", "Conocedor", "Maestro"), tiers.map { it.name })
        assertEquals(listOf(0, 300, 700), tiers.map { it.threshold })
        assertEquals(listOf(5, 10, 15), tiers.map { it.cafeDelDiaPct })
    }
}

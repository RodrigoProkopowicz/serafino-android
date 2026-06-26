package com.serafino.domain

import com.serafino.domain.entities.loyalty.LoyaltyAccount
import com.serafino.domain.entities.loyalty.LoyaltyTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Paridad con `LoyaltyAccountTests` de iOS (aritmética de nivel/progreso, lógica pura). */
class LoyaltyAccountTest {
    private val ladder = listOf(
        LoyaltyTier("a", "A", "", 0, perks = emptyList(), symbolName = "leaf", accentHex = "7E9B57"),
        LoyaltyTier("b", "B", "", 300, perks = emptyList(), symbolName = "flame", accentHex = "C0833F"),
        LoyaltyTier("c", "C", "", 800, perks = emptyList(), symbolName = "star", accentHex = "E0A766"),
        LoyaltyTier("d", "D", "", 1600, perks = emptyList(), symbolName = "crown", accentHex = "F0C674"),
    )

    private fun account(lifetime: Int) =
        LoyaltyAccount.make(points = lifetime, lifetimePoints = lifetime, amountPerPoint = 1000, tiers = ladder, history = emptyList())

    @Test fun currentTierByLifetime() {
        assertEquals("a", account(0).currentTier.id)
        assertEquals("a", account(299).currentTier.id)
        assertEquals("b", account(300).currentTier.id)
        assertEquals("c", account(1240).currentTier.id)
        assertEquals("d", account(5000).currentTier.id)
    }

    @Test fun nextTier() {
        assertEquals("d", account(1240).nextTier?.id)
        assertEquals("b", account(0).nextTier?.id)
        assertNull(account(1600).nextTier)
    }

    @Test fun beansToNext() {
        assertEquals(360, account(1240).beansToNextTier)
        assertEquals(500, account(300).beansToNextTier)
        assertEquals(0, account(2000).beansToNextTier)
    }

    @Test fun progressWithinTier() {
        assertEquals(0.55, account(1240).progress, 0.0001)
        assertEquals(0.0, account(800).progress, 0.0001)
        assertEquals(1.0, account(1600).progress, 0.0001)
    }

    @Test fun maxAndUnlocked() {
        val acc = account(1240)
        assertFalse(acc.isMaxTier)
        assertTrue(account(1600).isMaxTier)
        assertTrue(acc.isUnlocked(ladder[0]))
        assertTrue(acc.isUnlocked(ladder[2]))
        assertFalse(acc.isUnlocked(ladder[3]))
    }

    @Test fun backendTierKeyWins() {
        val acc = LoyaltyAccount(
            pointsName = "granos", points = 1000, lifetimePoints = 1000, amountPerPoint = 1000,
            tierKey = "b", tierLabel = "B", nextTierKey = "c", nextTierLabel = "C",
            pointsToNext = 50, toNextLabel = "Te faltan 50 granos para C", progressPercent = 50,
            tiers = ladder, history = emptyList(),
        )
        assertEquals("b", acc.currentTier.id)
        assertEquals("c", acc.nextTier?.id)
        assertEquals(50, acc.beansToNextTier)
    }

    @Test fun emptyLadderIsSafe() {
        val acc = LoyaltyAccount.make(points = 100, lifetimePoints = 100, amountPerPoint = 1000, tiers = emptyList(), history = emptyList())
        assertEquals(LoyaltyTier.placeholder.id, acc.currentTier.id)
        assertNull(acc.nextTier)
        assertTrue(acc.isMaxTier)
    }
}

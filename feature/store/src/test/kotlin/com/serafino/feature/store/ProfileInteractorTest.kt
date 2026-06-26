package com.serafino.feature.store

import com.serafino.architecture.AuthUser
import com.serafino.architecture.BeansEarnedEvent
import com.serafino.architecture.EventBus
import com.serafino.feature.store.profile.ProfileInteractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileInteractorTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun make(signedIn: Boolean): Triple<ProfileInteractor, MockAuthService, MockLoyaltyProvider> {
        val bus = EventBus()
        val user = if (signedIn) AuthUser("u1", "Ana", "ana@mail.com", testDate(2025, 1, 1)) else null
        val auth = MockAuthService(bus, user)
        val loyalty = MockLoyaltyProvider()
        val interactor = ProfileInteractor(auth, loyalty, bus)
        return Triple(interactor, auth, loyalty)
    }

    @Test fun signedOutShowsGate() {
        val (i, _, _) = make(signedIn = false)
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals(ProfileInteractor.State.SignedOut, i.state)
    }

    @Test fun signInLoadsLoyaltyAccount() {
        val (i, _, _) = make(signedIn = false)
        i.handle(ProfileInteractor.Input.OnAppear)
        i.handle(ProfileInteractor.Input.SignIn)
        assertEquals(ProfileInteractor.State.Loaded, i.state)
        assertEquals("450", i.data.balanceText)
        assertEquals("Intermedio", i.data.tier.name)
        assertEquals("Ana", i.data.memberName)
        assertTrue(i.data.activity.isNotEmpty())
    }

    @Test fun loadsImmediatelyWhenAlreadySignedIn() {
        val (i, _, _) = make(signedIn = true)
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals(ProfileInteractor.State.Loaded, i.state)
        assertEquals(3, i.data.tiers.size)
    }

    @Test fun signOutReturnsToGateAndClears() {
        val (i, _, _) = make(signedIn = true)
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals(ProfileInteractor.State.Loaded, i.state)
        i.handle(ProfileInteractor.Input.SignOut)
        assertEquals(ProfileInteractor.State.SignedOut, i.state)
        assertEquals("0", i.data.balanceText) // data reseteada
    }

    /** El historial usa etiquetas legibles; la reversión no se lee como compra. */
    @Test fun ledgerLabelsAreReadable() {
        val (i, _, _) = make(signedIn = true)
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals(ProfileInteractor.State.Loaded, i.state)
        // Movimientos ordenados por fecha desc: compra (más reciente), canje, reembolso.
        assertEquals(listOf("Compra", "Canje", "Reembolso"), i.data.activity.map { it.typeText })
        val reversal = i.data.activity.first { it.typeText == "Reembolso" }
        assertFalse(reversal.isCredit)
        // La reversión usa su propio ícono, distinto del de compra.
        assertEquals("undo", reversal.symbol)
        assertEquals("bag", i.data.activity.first { it.typeText == "Compra" }.symbol)
    }

    /** Sin sesión, el Perfil muestra el gate sin tocar la red (ni config pública ni cuenta). */
    @Test fun signedOutDoesNotFetch() {
        val (i, _, loyalty) = make(signedIn = false)
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals(ProfileInteractor.State.SignedOut, i.state)
        assertEquals(0, loyalty.loadAccountCount)
        assertEquals(0, loyalty.loadConfigCount)
        assertTrue(i.data.tiers.isEmpty())
    }

    @Test fun beansEarnedEventReloadsAccount() {
        val bus = EventBus()
        val auth = MockAuthService(bus, AuthUser("u1", "Ana", "ana@mail.com", testDate(2025, 1, 1)))
        val loyalty = MockLoyaltyProvider()
        val i = ProfileInteractor(auth, loyalty, bus)
        i.handle(ProfileInteractor.Input.OnAppear)
        val before = loyalty.loadAccountCount
        // Un evento de granos ganados (lo emite el backend tras una compra) fuerza recarga.
        bus.publish(BeansEarnedEvent(amount = 45))
        assertTrue(loyalty.loadAccountCount > before)
    }
}

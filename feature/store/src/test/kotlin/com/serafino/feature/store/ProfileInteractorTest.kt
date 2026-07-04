package com.serafino.feature.store

import com.serafino.architecture.AuthUser
import com.serafino.architecture.BeansEarnedEvent
import com.serafino.architecture.EventBus
import com.serafino.feature.store.profile.ProfileInteractor
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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

    // MARK: frescura

    private fun makeFresh(
        signedIn: Boolean,
        autoRefreshMillis: Long = 600_000,
        staleTTLMillis: Long = 0,
        reconcileDelaysMillis: List<Long> = listOf(10, 10, 10),
    ): Cuarteto {
        val bus = EventBus()
        val user = if (signedIn) AuthUser("u1", "Ana", "ana@mail.com", testDate(2025, 1, 1)) else null
        val auth = MockAuthService(bus, user)
        val loyalty = MockLoyaltyProvider()
        val interactor = ProfileInteractor(
            auth, loyalty, bus,
            autoRefreshInterval = autoRefreshMillis.milliseconds,
            staleTTL = staleTTLMillis.milliseconds,
            reconcileDelays = reconcileDelaysMillis.map { it.milliseconds },
            now = { Date(mainRule.dispatcher.scheduler.currentTime) },
        )
        return Cuarteto(interactor, auth, loyalty, bus)
    }

    private data class Cuarteto(
        val interactor: ProfileInteractor,
        val auth: MockAuthService,
        val loyalty: MockLoyaltyProvider,
        val bus: EventBus,
    )

    /** Reingresar al Perfil con el TTL vencido refresca con la carga liviana, sin parpadeo. */
    @Test fun reentryRefreshesSilently() = runTest {
        val (i, _, loyalty, _) = makeFresh(signedIn = true)
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals(ProfileInteractor.State.Loaded, i.state)
        assertEquals(1, loyalty.loadAccountCount)

        // Reingreso al tab (otro OnAppear): refresca con el resumen (1 GET, no 3)…
        advanceTimeBy(1)
        i.handle(ProfileInteractor.Input.OnAppear)
        // …sin pasar por Loading (no debe haber parpadeo del skeleton).
        assertEquals(ProfileInteractor.State.Loaded, i.state)
        assertEquals(1, loyalty.summaryCount)
        assertEquals(1, loyalty.loadAccountCount)
    }

    /** Reingresar dentro del TTL no toca la red. */
    @Test fun reentryWithinTTLSkipsNetwork() = runTest {
        val (i, _, loyalty, _) = makeFresh(signedIn = true, staleTTLMillis = 60_000)
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals(1, loyalty.loadAccountCount)

        advanceTimeBy(1000)
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals(1, loyalty.loadAccountCount)
        assertEquals(0, loyalty.summaryCount)
        assertEquals(ProfileInteractor.State.Loaded, i.state)
    }

    /** Los triggers concurrentes del arranque comparten UNA sola carga (dedupe en vuelo). */
    @Test fun concurrentStartupLoadsAreCoalesced() = runTest {
        val (i, _, loyalty, bus) = makeFresh(signedIn = true)
        loyalty.delayMillis = 50

        // Arranque real: el listener de sesión publica AuthChanged y la vista dispara OnAppear.
        bus.publish(com.serafino.architecture.AuthChangedEvent(isSignedIn = true))
        i.handle(ProfileInteractor.Input.OnAppear)
        advanceUntilIdle()

        assertEquals(1, loyalty.loadAccountCount + loyalty.summaryCount)
        assertEquals(ProfileInteractor.State.Loaded, i.state)
    }

    /** Auto-refresca la cuenta (liviano) mientras el Perfil está visible, y para al cancelar. */
    @Test fun autoRefreshReloadsUntilCancelled() = runTest {
        val (i, _, loyalty, _) = makeFresh(signedIn = true, autoRefreshMillis = 40)
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals(1, loyalty.loadAccountCount)

        // El LaunchedEffect de la pantalla corre el bucle mientras el tab está visible…
        val job = launch { i.autoRefresh() }
        advanceTimeBy(150); runCurrent()
        job.cancel()
        val ticks = loyalty.summaryCount
        assertTrue(ticks >= 2)                       // refrescó varias veces con la carga liviana
        assertEquals(1, loyalty.loadAccountCount)    // sin cargas completas ni parpadeo
        assertEquals(ProfileInteractor.State.Loaded, i.state)

        // …y al salir del tab (cancelación) deja de pegarle al backend.
        advanceTimeBy(200); runCurrent()
        assertEquals(ticks, loyalty.summaryCount)
    }

    /** El auto-refresco sin sesión no toca la red. */
    @Test fun autoRefreshSignedOutDoesNothing() = runTest {
        val (i, _, loyalty, _) = makeFresh(signedIn = false, autoRefreshMillis = 30)
        i.handle(ProfileInteractor.Input.OnAppear)

        val job = launch { i.autoRefresh() }
        advanceTimeBy(150); runCurrent()
        job.cancel()

        assertEquals(0, loyalty.loadAccountCount)
        assertEquals(0, loyalty.summaryCount)
        assertEquals(ProfileInteractor.State.SignedOut, i.state)
    }

    /** Una recarga de mutación no se conforma con un resumen en vuelo: relanza la completa. */
    @Test fun mutationReloadIsNotSatisfiedByInFlightSummary() = runTest {
        val (i, _, loyalty, bus) = makeFresh(signedIn = true)
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals(1, loyalty.loadAccountCount)

        // Reingreso al tab (TTL cero → resumen liviano) y, con ese resumen EN VUELO,
        // se concreta un canje: la mutación necesita escalera + historial frescos.
        loyalty.delayMillis = 100
        advanceTimeBy(1)
        i.handle(ProfileInteractor.Input.OnAppear)
        advanceTimeBy(20); runCurrent()   // el resumen ya está en vuelo
        bus.publish(com.serafino.architecture.RewardRedeemedEvent())
        advanceUntilIdle()

        // El resumen corrió, pero el canje NO se satisfizo con él: relanzó la completa.
        assertEquals(1, loyalty.summaryCount)
        assertEquals(2, loyalty.loadAccountCount)
        assertEquals(ProfileInteractor.State.Loaded, i.state)
    }

    /** Cerrar sesión con una carga en vuelo no resucita los datos del usuario anterior. */
    @Test fun signOutDuringInFlightLoadKeepsGate() = runTest {
        val (i, _, loyalty, _) = makeFresh(signedIn = true)
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals(ProfileInteractor.State.Loaded, i.state)

        // Una recarga lenta en vuelo…
        loyalty.delayMillis = 120
        val refresh = launch { i.refresh() }
        advanceTimeBy(20); runCurrent()
        // …y el usuario cierra sesión antes de que el backend responda.
        i.handle(ProfileInteractor.Input.SignOut)
        assertEquals(ProfileInteractor.State.SignedOut, i.state)

        // Al llegar la respuesta tardía, el gate NO se pisa con la cuenta anterior.
        advanceUntilIdle()
        refresh.cancel()
        assertEquals(ProfileInteractor.State.SignedOut, i.state)
        assertTrue(i.data.memberName.isEmpty())
        assertEquals("0", i.data.balanceText)
    }

    /** Una compra aprobada reconcilia contra el webhook y corta al ver el saldo nuevo. */
    @Test fun purchaseReconciliationStopsWhenBalanceChanges() = runTest {
        val (i, _, loyalty, bus) = makeFresh(signedIn = true)
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals("450", i.data.balanceText)
        assertEquals(1, loyalty.loadAccountCount)

        // El webhook acredita antes del primer reintento: un solo reload extra alcanza.
        loyalty.account = SampleLoyalty.account(points = 495, lifetime = 495)
        bus.publish(com.serafino.architecture.PurchaseCompletedEvent("order-1"))
        advanceUntilIdle()

        assertEquals("495", i.data.balanceText)
        assertEquals(2, loyalty.loadAccountCount)
    }

    /** Si el webhook nunca acredita, la reconciliación agota sus reintentos y para. */
    @Test fun purchaseReconciliationExhaustsRetries() = runTest {
        val (i, _, loyalty, bus) = makeFresh(signedIn = true)
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals(1, loyalty.loadAccountCount)

        // El saldo no cambia: reintenta una vez por delay (3) y no sigue pegando al backend.
        bus.publish(com.serafino.architecture.PurchaseCompletedEvent("order-1"))
        advanceUntilIdle()
        assertEquals(4, loyalty.loadAccountCount)
    }

    /** RewardRedeemedEvent refresca la cuenta completa. */
    @Test fun rewardRedeemedTriggersReload() = runTest {
        val (i, _, loyalty, bus) = makeFresh(signedIn = true)
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals(1, loyalty.loadAccountCount)

        bus.publish(com.serafino.architecture.RewardRedeemedEvent())
        advanceUntilIdle()
        assertEquals(2, loyalty.loadAccountCount)
        assertEquals(ProfileInteractor.State.Loaded, i.state)
    }

    /** La vuelta al foreground refresca (liviano) solo si el TTL venció. */
    @Test fun foregroundRefreshIsTTLGated() = runTest {
        // TTL cero: al volver al foreground el dato ya venció → resumen.
        val (stale, _, loyaltyStale, busStale) = makeFresh(signedIn = true)
        stale.handle(ProfileInteractor.Input.OnAppear)
        advanceTimeBy(1)
        busStale.publish(com.serafino.architecture.AppDidBecomeActiveEvent())
        runCurrent()
        assertEquals(1, loyaltyStale.summaryCount)
        assertEquals(1, loyaltyStale.loadAccountCount)

        // TTL amplio: un background corto no cuesta red.
        val (fresh, _, loyaltyFresh, busFresh) = makeFresh(signedIn = true, staleTTLMillis = 60_000)
        fresh.handle(ProfileInteractor.Input.OnAppear)
        advanceTimeBy(1000)
        busFresh.publish(com.serafino.architecture.AppDidBecomeActiveEvent())
        runCurrent()
        assertEquals(0, loyaltyFresh.summaryCount)
        assertEquals(1, loyaltyFresh.loadAccountCount)
    }

    /** La vuelta al foreground sin sesión no toca la red. */
    @Test fun foregroundSignedOutDoesNothing() = runTest {
        val (i, _, loyalty, bus) = makeFresh(signedIn = false)
        i.handle(ProfileInteractor.Input.OnAppear)

        bus.publish(com.serafino.architecture.AppDidBecomeActiveEvent())
        runCurrent()
        assertEquals(0, loyalty.loadAccountCount)
        assertEquals(0, loyalty.summaryCount)
        assertEquals(ProfileInteractor.State.SignedOut, i.state)
    }

    /** Pull-to-refresh re-pide la cuenta y mantiene el contenido (sin Loading). */
    @Test fun pullToRefreshReloads() = runTest {
        val (i, _, loyalty, _) = makeFresh(signedIn = true)
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals(1, loyalty.loadAccountCount)

        i.refresh()
        assertEquals(2, loyalty.loadAccountCount)
        assertEquals(ProfileInteractor.State.Loaded, i.state)
    }

    /** Pull-to-refresh sin sesión no toca la red. */
    @Test fun pullToRefreshSignedOutDoesNothing() = runTest {
        val (i, _, loyalty, _) = makeFresh(signedIn = false)
        i.handle(ProfileInteractor.Input.OnAppear)

        i.refresh()
        assertEquals(0, loyalty.loadAccountCount)
        assertEquals(ProfileInteractor.State.SignedOut, i.state)
    }

    // MARK: borrado de cuenta

    private data class DeleteSetup(
        val interactor: ProfileInteractor,
        val auth: MockAuthService,
        val account: MockAccountService,
    )

    private fun makeDelete(accountError: Throwable? = null): DeleteSetup {
        val bus = EventBus()
        val auth = MockAuthService(bus, AuthUser("u1", "Ana", "ana@mail.com", testDate(2025, 1, 1)))
        val loyalty = MockLoyaltyProvider()
        val account = MockAccountService(accountError)
        val interactor = ProfileInteractor(auth, loyalty, bus, account = account)
        return DeleteSetup(interactor, auth, account)
    }

    /** Tocar "Eliminar cuenta" abre la confirmación (sin tocar el backend). */
    @Test fun deleteRequestOpensConfirmation() {
        val (i, _, account) = makeDelete()
        i.handle(ProfileInteractor.Input.OnAppear)
        i.handle(ProfileInteractor.Input.DeleteAccountRequested)
        assertTrue(i.data.confirmingDelete)
        assertEquals(0, account.deleteCount)
    }

    /** Cancelar cierra la confirmación sin borrar nada. */
    @Test fun deleteCancelClosesConfirmation() {
        val (i, auth, account) = makeDelete()
        i.handle(ProfileInteractor.Input.OnAppear)
        i.handle(ProfileInteractor.Input.DeleteAccountRequested)
        i.handle(ProfileInteractor.Input.DeleteAccountCancelled)
        assertFalse(i.data.confirmingDelete)
        assertEquals(0, account.deleteCount)
        assertEquals(0, auth.signOutCount)
        assertEquals(ProfileInteractor.State.Loaded, i.state)
    }

    /** Confirmar borra en el backend y, con éxito, cierra la sesión local y vuelve al gate. */
    @Test fun deleteConfirmedCallsBackendThenSignsOut() {
        val (i, auth, account) = makeDelete()
        i.handle(ProfileInteractor.Input.OnAppear)
        assertEquals("450", i.data.balanceText)

        i.handle(ProfileInteractor.Input.DeleteAccountRequested)
        i.handle(ProfileInteractor.Input.DeleteAccountConfirmed)

        assertEquals(1, account.deleteCount)
        assertEquals(1, auth.signOutCount)          // limpieza local tras el borrado
        assertEquals(ProfileInteractor.State.SignedOut, i.state)
        assertEquals("0", i.data.balanceText)       // data reseteada por clear()
        assertFalse(i.data.confirmingDelete)
        assertFalse(i.data.deleting)
    }

    /**
     * Si el borrado en el backend fue OK pero el `signOut` LOCAL lanza (la cuenta ya no existe),
     * igual volvemos al gate sin dejar el modal trabado en el spinner (regresión de revisión).
     */
    @Test fun deleteReturnsToGateEvenIfLocalSignOutThrows() {
        val bus = EventBus()
        val auth = MockAuthService(bus, AuthUser("u1", "Ana", "ana@mail.com", testDate(2025, 1, 1)))
            .apply { signOutError = RuntimeException("firebase signOut boom") }
        val account = MockAccountService()
        val i = ProfileInteractor(auth, MockLoyaltyProvider(), bus, account = account)
        i.handle(ProfileInteractor.Input.OnAppear)
        i.handle(ProfileInteractor.Input.DeleteAccountRequested)
        i.handle(ProfileInteractor.Input.DeleteAccountConfirmed)

        assertEquals(1, account.deleteCount)        // el backend borró (irreversible)
        assertEquals(1, auth.signOutCount)          // se intentó cerrar sesión (y lanzó)
        assertEquals(ProfileInteractor.State.SignedOut, i.state)   // igual salimos al gate
        assertFalse(i.data.deleting)                // spinner apagado
        assertFalse(i.data.confirmingDelete)        // modal cerrado
    }

    /** Si el backend falla, NO cierra la sesión: conserva el contenido y muestra el error para reintentar. */
    @Test fun deleteFailureKeepsSessionAndShowsError() {
        val (i, auth, account) = makeDelete(accountError = RuntimeException("No se pudo eliminar la cuenta."))
        i.handle(ProfileInteractor.Input.OnAppear)
        i.handle(ProfileInteractor.Input.DeleteAccountRequested)
        i.handle(ProfileInteractor.Input.DeleteAccountConfirmed)

        assertEquals(1, account.deleteCount)
        assertEquals(0, auth.signOutCount)          // la sesión sobrevive al fallo
        assertEquals(ProfileInteractor.State.Loaded, i.state)
        assertEquals("No se pudo eliminar la cuenta.", i.data.deleteError)
        assertFalse(i.data.deleting)
        assertTrue(i.data.confirmingDelete)         // el modal sigue abierto para reintentar
    }

    /** Sin servicio de borrado cableado, muestra un error y no cierra la sesión. */
    @Test fun deleteWithoutServiceShowsError() {
        val bus = EventBus()
        val auth = MockAuthService(bus, AuthUser("u1", "Ana", "ana@mail.com", testDate(2025, 1, 1)))
        val i = ProfileInteractor(auth, MockLoyaltyProvider(), bus, account = null)
        i.handle(ProfileInteractor.Input.OnAppear)
        i.handle(ProfileInteractor.Input.DeleteAccountRequested)
        i.handle(ProfileInteractor.Input.DeleteAccountConfirmed)

        assertEquals(0, auth.signOutCount)
        assertEquals(ProfileInteractor.State.Loaded, i.state)
        assertTrue(i.data.deleteError != null)
    }

    /** Durante el borrado, `deleting` marca el spinner y aún no cerró sesión; al terminar, sale al gate. */
    @Test fun deleteShowsSpinnerWhileInFlight() = runTest {
        val bus = EventBus()
        val auth = MockAuthService(bus, AuthUser("u1", "Ana", "ana@mail.com", testDate(2025, 1, 1)))
        val account = MockAccountService().apply { delayMillis = 50 }
        val i = ProfileInteractor(auth, MockLoyaltyProvider(), bus, account = account)
        i.handle(ProfileInteractor.Input.OnAppear)
        i.handle(ProfileInteractor.Input.DeleteAccountRequested)
        i.handle(ProfileInteractor.Input.DeleteAccountConfirmed)

        runCurrent()
        assertTrue(i.data.deleting)                 // request en vuelo
        assertEquals(0, auth.signOutCount)

        advanceUntilIdle()
        assertEquals(1, account.deleteCount)
        assertEquals(1, auth.signOutCount)
        assertEquals(ProfileInteractor.State.SignedOut, i.state)
    }

    /** Un segundo "Confirmar" mientras el borrado está en vuelo no dispara una segunda request. */
    @Test fun deleteIgnoresReentrantConfirmWhileInFlight() = runTest {
        val bus = EventBus()
        val auth = MockAuthService(bus, AuthUser("u1", "Ana", "ana@mail.com", testDate(2025, 1, 1)))
        val account = MockAccountService().apply { delayMillis = 50 }
        val i = ProfileInteractor(auth, MockLoyaltyProvider(), bus, account = account)
        i.handle(ProfileInteractor.Input.OnAppear)
        i.handle(ProfileInteractor.Input.DeleteAccountRequested)
        i.handle(ProfileInteractor.Input.DeleteAccountConfirmed)
        runCurrent()
        i.handle(ProfileInteractor.Input.DeleteAccountConfirmed)   // doble tap
        advanceUntilIdle()
        assertEquals(1, account.deleteCount)
    }
}

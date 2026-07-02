package com.serafino.feature.store

import com.serafino.architecture.EventBus
import com.serafino.domain.entities.store.RoastFilter
import com.serafino.feature.store.catalog.StoreCatalogInteractor
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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

    // MARK: frescura

    private fun makeFresh(
        catalog: MockProductCatalog = MockProductCatalog(),
        staleTTLMillis: Long = 0,
        autoRefreshMillis: Long = 300_000,
    ): Triple<StoreCatalogInteractor, MockCartStore, EventBus> {
        val bus = EventBus()
        val cart = MockCartStore(bus)
        val interactor = StoreCatalogInteractor(
            catalog, cart, bus,
            autoRefreshInterval = autoRefreshMillis.milliseconds,
            staleTTL = staleTTLMillis.milliseconds,
            now = { Date(mainRule.dispatcher.scheduler.currentTime) },
        )
        return Triple(interactor, cart, bus)
    }

    /** Reingresar a la Tienda con el TTL vencido refresca en segundo plano, sin parpadeo. */
    @Test fun reentryRefreshesSilently() = runTest {
        val catalog = MockProductCatalog()
        val (i, _, _) = makeFresh(catalog, staleTTLMillis = 0)
        i.handle(StoreCatalogInteractor.Input.OnAppear)
        assertEquals(StoreCatalogInteractor.State.Loaded, i.state)
        assertEquals(1, catalog.loadCount)

        // Reingreso al tab (otro OnAppear): vuelve a pedir el catálogo para mantenerlo al día…
        advanceTimeBy(1)
        i.handle(StoreCatalogInteractor.Input.OnAppear)
        // …sin pasar por Loading (no debe haber parpadeo del skeleton).
        assertEquals(StoreCatalogInteractor.State.Loaded, i.state)
        assertEquals(2, catalog.loadCount)
    }

    /** Reingresar dentro del TTL no consulta la red (alternar tabs rápido es gratis). */
    @Test fun reentryWithinTTLSkipsNetwork() = runTest {
        val catalog = MockProductCatalog()
        val (i, _, _) = makeFresh(catalog, staleTTLMillis = 60_000)
        i.handle(StoreCatalogInteractor.Input.OnAppear)
        assertEquals(1, catalog.loadCount)

        advanceTimeBy(1000)
        i.handle(StoreCatalogInteractor.Input.OnAppear)
        assertEquals(1, catalog.loadCount)
        assertEquals(StoreCatalogInteractor.State.Loaded, i.state)
    }

    /** Pull-to-refresh fuerza la recarga aunque el TTL siga vigente. */
    @Test fun pullToRefreshForcesReload() = runTest {
        val catalog = MockProductCatalog()
        val (i, _, _) = makeFresh(catalog, staleTTLMillis = 60_000)
        i.handle(StoreCatalogInteractor.Input.OnAppear)
        assertEquals(1, catalog.loadCount)

        i.refresh()
        assertEquals(2, catalog.loadCount)
        assertEquals(StoreCatalogInteractor.State.Loaded, i.state)
    }

    /** La vuelta al foreground refresca solo si el TTL venció. */
    @Test fun foregroundRefreshIsTTLGated() = runTest {
        // TTL cero: al volver al foreground el catálogo ya está vencido → refresca.
        val catalog = MockProductCatalog()
        val (stale, _, bus) = makeFresh(catalog, staleTTLMillis = 0)
        stale.handle(StoreCatalogInteractor.Input.OnAppear)
        assertEquals(1, catalog.loadCount)
        advanceTimeBy(1)
        bus.publish(com.serafino.architecture.AppDidBecomeActiveEvent())
        assertEquals(2, catalog.loadCount)

        // TTL amplio: un background corto no cuesta red.
        val catalog2 = MockProductCatalog()
        val (fresh, _, bus2) = makeFresh(catalog2, staleTTLMillis = 60_000)
        fresh.handle(StoreCatalogInteractor.Input.OnAppear)
        assertEquals(1, catalog2.loadCount)
        advanceTimeBy(1000)
        bus2.publish(com.serafino.architecture.AppDidBecomeActiveEvent())
        assertEquals(1, catalog2.loadCount)
    }

    /** Con caché previa arranca mostrando el catálogo al instante y revalida por red. */
    @Test fun startsFromCachedSnapshot() = runTest {
        val catalog = MockProductCatalog()
        catalog.seeded = SampleProducts.all.filter { !it.hidden }
        catalog.seededDate = testDate(2026, 6, 30)
        catalog.delayMillis = 10   // la revalidación tarda: se observa el estado intermedio
        val (i, _, _) = makeFresh(catalog)

        i.handle(StoreCatalogInteractor.Input.OnAppear)
        // Sin esperar la red: el snapshot ya se muestra (nada de skeleton) con su fecha.
        assertEquals(StoreCatalogInteractor.State.Loaded, i.state)
        assertTrue(i.data.products.isNotEmpty())
        assertEquals(testDate(2026, 6, 30), i.lastUpdatedAt)

        // Y detrás revalida por red una sola vez.
        advanceTimeBy(10); runCurrent()
        assertEquals(1, catalog.loadCount)
        assertTrue(i.lastUpdatedAt != testDate(2026, 6, 30))
    }

    /** Un fallo del refresco en segundo plano conserva el contenido en pantalla. */
    @Test fun backgroundFailureKeepsContent() = runTest {
        val catalog = MockProductCatalog()
        val (i, _, _) = makeFresh(catalog, staleTTLMillis = 0)
        i.handle(StoreCatalogInteractor.Input.OnAppear)
        assertEquals(StoreCatalogInteractor.State.Loaded, i.state)
        val before = i.lastUpdatedAt

        // El refresco del reingreso falla: el catálogo anterior queda y la fecha no avanza.
        catalog.error = RuntimeException("sin red")
        advanceTimeBy(1)
        i.handle(StoreCatalogInteractor.Input.OnAppear)
        assertEquals(StoreCatalogInteractor.State.Loaded, i.state)
        assertTrue(i.data.products.isNotEmpty())
        assertEquals(before, i.lastUpdatedAt)
    }

    /** Auto-refresca el catálogo mientras está visible, y se detiene al cancelar (salir del tab). */
    @Test fun autoRefreshReloadsUntilCancelled() = runTest {
        val catalog = MockProductCatalog()
        val (i, _, _) = makeFresh(catalog, autoRefreshMillis = 20)
        i.handle(StoreCatalogInteractor.Input.OnAppear)
        assertEquals(1, catalog.loadCount)

        // Mientras el LaunchedEffect vive, el catálogo se vuelve a pedir cada intervalo…
        val job = launch { i.autoRefresh() }
        advanceTimeBy(150); runCurrent()
        assertTrue(catalog.loadCount >= 2)

        // …y al salir del tab (cancelación) deja de consultar al backend.
        job.cancel()
        val afterCancel = catalog.loadCount
        advanceTimeBy(200); runCurrent()
        assertEquals(afterCancel, catalog.loadCount)
    }
}

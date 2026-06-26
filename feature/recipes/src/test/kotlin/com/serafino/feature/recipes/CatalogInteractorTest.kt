package com.serafino.feature.recipes

import com.serafino.architecture.EventBus
import com.serafino.domain.entities.BrewMethod
import com.serafino.feature.recipes.catalog.CatalogInteractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Paridad con `CatalogInteractorTests` de iOS. */
class CatalogInteractorTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun make(favorites: Set<String> = emptySet()): Triple<CatalogInteractor, MockFavoritesStore, EventBus> {
        val bus = EventBus()
        val store = MockFavoritesStore(bus, favorites)
        return Triple(CatalogInteractor(MockRecipeCatalog(), store, bus), store, bus)
    }

    private fun ids(i: CatalogInteractor) = i.data.sections.flatMap { it.items }.map { it.id }

    @Test fun loadsAndGroupsByMethodSortIndex() {
        val (i, _, _) = make()
        i.handle(CatalogInteractor.Input.OnAppear)
        assertEquals(CatalogInteractor.State.Loaded, i.state)
        assertEquals(listOf(BrewMethod.Espresso, BrewMethod.PourOverV60, BrewMethod.FrenchPress), i.data.sections.map { it.method })
        assertEquals("Test Espresso", i.data.sections.first().items.first().name)
    }

    @Test fun reflectsInitialFavorites() {
        val (i, _, _) = make(favorites = setOf("test-v60"))
        i.handle(CatalogInteractor.Input.OnAppear)
        val v60 = i.data.sections.flatMap { it.items }.first { it.id == "test-v60" }
        assertTrue(v60.isFavorite)
    }

    @Test fun searchMatchesNameMethodAndFlavorNotes() {
        val (i, _, _) = make()
        i.handle(CatalogInteractor.Input.OnAppear)
        i.handle(CatalogInteractor.Input.Search("floral")) // nota de sabor del V60
        assertEquals(listOf(BrewMethod.PourOverV60), i.data.sections.map { it.method })
        i.handle(CatalogInteractor.Input.Search("french")) // nombre French Press
        assertEquals(listOf(BrewMethod.FrenchPress), i.data.sections.map { it.method })
    }

    @Test fun searchNoMatchIsEmpty() {
        val (i, _, _) = make()
        i.handle(CatalogInteractor.Input.OnAppear)
        i.handle(CatalogInteractor.Input.Search("zzzzz"))
        assertEquals(CatalogInteractor.State.Empty, i.state)
        assertTrue(i.data.sections.isEmpty())
    }

    @Test fun clearingSearchRestoresAll() {
        val (i, _, _) = make()
        i.handle(CatalogInteractor.Input.OnAppear)
        i.handle(CatalogInteractor.Input.Search("floral"))
        i.handle(CatalogInteractor.Input.Search(""))
        assertEquals(3, i.data.sections.size)
        assertEquals(CatalogInteractor.State.Loaded, i.state)
    }

    @Test fun toggleFavoritePersistsAndUpdatesRow() {
        val (i, store, _) = make()
        i.handle(CatalogInteractor.Input.OnAppear)
        i.handle(CatalogInteractor.Input.ToggleFavorite("test-espresso"))
        assertTrue(store.isFavorite("test-espresso"))
        val row = i.data.sections.flatMap { it.items }.first { it.id == "test-espresso" }
        assertTrue(row.isFavorite)
    }

    @Test fun reactsToExternalFavoriteEvent() {
        val (i, store, _) = make()
        i.handle(CatalogInteractor.Input.OnAppear)
        store.setFavorite("test-v60", true) // como si se tocara desde otro módulo
        val row = i.data.sections.flatMap { it.items }.first { it.id == "test-v60" }
        assertTrue(row.isFavorite)
    }

    @Test fun favoritesFilterShowsOnlyFavorites() {
        val (i, _, _) = make(favorites = setOf("test-v60"))
        i.handle(CatalogInteractor.Input.OnAppear)
        assertFalse(i.data.showFavoritesOnly)
        i.handle(CatalogInteractor.Input.ToggleFavoritesFilter)
        assertTrue(i.data.showFavoritesOnly)
        assertTrue(i.data.hasFavorites)
        assertEquals(listOf("test-v60"), ids(i))
    }

    @Test fun favoritesFilterEmptyWhenNoFavorites() {
        val (i, _, _) = make()
        i.handle(CatalogInteractor.Input.OnAppear)
        i.handle(CatalogInteractor.Input.ToggleFavoritesFilter)
        assertEquals(CatalogInteractor.State.Empty, i.state)
        assertTrue(i.data.sections.isEmpty())
        assertFalse(i.data.hasFavorites)
    }

    @Test fun unfavoriteDropsRowInFilter() {
        val (i, store, _) = make(favorites = setOf("test-v60", "test-espresso"))
        i.handle(CatalogInteractor.Input.OnAppear)
        i.handle(CatalogInteractor.Input.ToggleFavoritesFilter)
        assertEquals(2, i.data.sections.flatMap { it.items }.size)
        store.setFavorite("test-v60", false)
        assertEquals(listOf("test-espresso"), ids(i))
    }

    @Test fun toggleFilterOffRestoresAll() {
        val (i, _, _) = make(favorites = setOf("test-v60"))
        i.handle(CatalogInteractor.Input.OnAppear)
        i.handle(CatalogInteractor.Input.ToggleFavoritesFilter)
        assertEquals(1, i.data.sections.flatMap { it.items }.size)
        i.handle(CatalogInteractor.Input.ToggleFavoritesFilter)
        assertEquals(3, i.data.sections.size)
    }
}

package com.serafino.data.di

import android.content.Context
import com.serafino.architecture.DependencyContainer
import com.serafino.architecture.DependencyModule
import com.serafino.architecture.DependencyScope
import com.serafino.architecture.EventBus
import com.serafino.architecture.register
import com.serafino.architecture.require
import com.serafino.data.catalog.StaticRecipeCatalog
import com.serafino.data.persistence.AppPersistence
import com.serafino.data.persistence.RoomFavoritesStore
import com.serafino.data.settings.SharedPrefsCheckoutProfileStore
import com.serafino.data.settings.SharedPrefsSettingsStore
import com.serafino.domain.services.CheckoutProfileStoring
import com.serafino.domain.services.FavoritesStoring
import com.serafino.domain.services.RecipeCatalogProviding
import com.serafino.domain.services.SettingsStoring

/**
 * Registra los servicios base de la app en el [DependencyContainer]. Todos singletons para
 * compartir favoritos/ajustes entre módulos. Espeja `CoreModule` de iOS (toma `Context` en lugar
 * del `ModelContext` de SwiftData).
 */
class CoreModule(private val context: Context) : DependencyModule {

    override fun register(container: DependencyContainer) {
        container.register<EventBus>(DependencyScope.Singleton) { EventBus() }

        container.register<RecipeCatalogProviding>(DependencyScope.Singleton) { StaticRecipeCatalog() }

        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        container.register<SettingsStoring>(DependencyScope.Singleton) { resolver ->
            SharedPrefsSettingsStore(prefs, resolver.require<EventBus>())
        }

        container.register<CheckoutProfileStoring>(DependencyScope.Singleton) {
            SharedPrefsCheckoutProfileStore(prefs)
        }

        val dao = AppPersistence.database(context).favoritesDao()
        container.register<FavoritesStoring>(DependencyScope.Singleton) { resolver ->
            RoomFavoritesStore(dao, resolver.require<EventBus>())
        }
    }

    private companion object {
        const val PREFS = "serafino.prefs"
    }
}

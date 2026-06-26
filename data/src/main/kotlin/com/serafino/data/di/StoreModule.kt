package com.serafino.data.di

import android.content.Context
import com.serafino.architecture.AuthProviding
import com.serafino.architecture.DependencyContainer
import com.serafino.architecture.DependencyModule
import com.serafino.architecture.DependencyScope
import com.serafino.architecture.EventBus
import com.serafino.architecture.register
import com.serafino.architecture.require
import com.serafino.architecture.resolve
import com.serafino.data.persistence.AppPersistence
import com.serafino.data.persistence.RoomCartStore
import com.serafino.data.store.CoffeeStoreAPIClient
import com.serafino.data.store.FirestoreProductCatalog
import com.serafino.domain.services.CartStoring
import com.serafino.domain.services.CheckoutService
import com.serafino.domain.services.ProductCatalogProviding
import com.serafino.domain.services.ReviewsProviding

/**
 * Registra los servicios de la tienda. Todos singletons para compartir el catálogo cacheado y el
 * carrito entre módulos. Reusa el EventBus de [CoreModule]. Espeja `StoreModule` de iOS.
 */
class StoreModule(private val context: Context) : DependencyModule {

    override fun register(container: DependencyContainer) {
        container.register<ProductCatalogProviding>(DependencyScope.Singleton) { FirestoreProductCatalog() }

        val dao = AppPersistence.database(context).cartDao()
        container.register<CartStoring>(DependencyScope.Singleton) { resolver ->
            RoomCartStore(dao, resolver.require<EventBus>())
        }

        // Un único cliente cumple ambos roles (CheckoutService + ReviewsProviding).
        container.register<CoffeeStoreAPIClient>(DependencyScope.Singleton) { resolver ->
            CoffeeStoreAPIClient(auth = resolver.resolve<AuthProviding>())
        }
        container.register<CheckoutService>(DependencyScope.Singleton) { resolver ->
            resolver.require<CoffeeStoreAPIClient>()
        }
        container.register<ReviewsProviding>(DependencyScope.Singleton) { resolver ->
            resolver.require<CoffeeStoreAPIClient>()
        }
    }
}

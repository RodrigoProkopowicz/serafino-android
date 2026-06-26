package com.serafino.architecture

import androidx.compose.runtime.mutableStateListOf

/**
 * Destinos navegables de la app. Cada caso identifica un módulo VIPER que el `ModuleFactory`
 * sabe construir. Espeja el `AppRoute` de iOS.
 */
sealed interface AppRoute {
    data class RecipeDetail(val recipeID: String) : AppRoute
    data class BrewTimer(val recipeID: String) : AppRoute

    // Tienda
    data class ProductDetail(val productID: String) : AppRoute
    data object Cart : AppRoute
    data object Checkout : AppRoute
    data class OrderStatus(val orderID: String) : AppRoute

    // Fidelidad
    data object LoyaltyRedeem : AppRoute
}

/**
 * Pantallas raíz, una por pestaña del tab bar. Favoritos no es pestaña: vive como filtro
 * dentro de Recetas (`Catalog`).
 */
enum class RootScreen { Store, Catalog, Profile, Settings }

/** Rol de navegación de VIPER. El presenter le pide rutas; nunca toca la vista. */
interface RouterType {
    fun push(route: AppRoute)
    fun pop()
    fun popToRoot()
}

/**
 * Router observable que respalda una pila de navegación. Se crea uno por pestaña para que cada
 * flujo tenga su propia pila independiente, igual que un `NavigationStack` por tab en iOS.
 *
 * [path] es una lista de snapshot state de Compose: la UI la observa y se recompone al
 * cambiar (el análogo del `@Observable var path` de iOS).
 */
class Router : RouterType {
    val path = mutableStateListOf<AppRoute>()

    override fun push(route: AppRoute) {
        path.add(route)
    }

    override fun pop() {
        if (path.isNotEmpty()) path.removeAt(path.lastIndex)
    }

    override fun popToRoot() {
        path.clear()
    }
}

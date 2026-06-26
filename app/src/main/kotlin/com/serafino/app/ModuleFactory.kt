package com.serafino.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serafino.architecture.AppRoute
import com.serafino.architecture.DependencyContainer
import com.serafino.architecture.RootScreen
import com.serafino.architecture.Router
import com.serafino.designsystem.AppBackground
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.feature.recipes.catalog.CatalogModule
import com.serafino.feature.recipes.catalog.CatalogScreen
import com.serafino.feature.recipes.detail.RecipeDetailModule
import com.serafino.feature.recipes.detail.RecipeDetailScreen
import com.serafino.feature.recipes.settings.SettingsModule
import com.serafino.feature.recipes.settings.SettingsScreen
import com.serafino.feature.recipes.timer.BrewTimerModule
import com.serafino.feature.recipes.timer.BrewTimerScreen
import com.serafino.feature.store.cart.CartModule
import com.serafino.feature.store.cart.CartScreen
import com.serafino.feature.store.catalog.StoreCatalogModule
import com.serafino.feature.store.catalog.StoreCatalogScreen
import com.serafino.feature.store.checkout.CheckoutModule
import com.serafino.feature.store.checkout.CheckoutScreen
import com.serafino.feature.store.detail.ProductDetailModule
import com.serafino.feature.store.detail.ProductDetailScreen
import com.serafino.feature.store.order.OrderStatusModule
import com.serafino.feature.store.order.OrderStatusScreen
import com.serafino.feature.store.profile.ProfileModule
import com.serafino.feature.store.profile.ProfileScreen
import com.serafino.feature.store.redeem.RedeemModule
import com.serafino.feature.store.redeem.RedeemScreen

/**
 * Construye las pantallas VIPER bajo demanda resolviendo dependencias del container. Único lugar
 * que sabe cómo las rutas mapean a módulos concretos. Espeja `ModuleFactory` de iOS.
 */
class ModuleFactory(private val container: DependencyContainer) {

    @Composable
    fun Root(screen: RootScreen, router: Router) {
        when (screen) {
            RootScreen.Catalog -> {
                val presenter = remember { CatalogModule.build(container, router) }
                DisposableEffect(presenter) { onDispose { presenter.interactor.dispose() } }
                CatalogScreen(presenter)
            }
            RootScreen.Settings -> {
                val presenter = remember { SettingsModule.build(container) }
                SettingsScreen(presenter)
            }
            RootScreen.Store -> {
                val presenter = remember { StoreCatalogModule.build(container, router) }
                DisposableEffect(presenter) { onDispose { presenter.interactor.dispose() } }
                StoreCatalogScreen(presenter)
            }
            RootScreen.Profile -> {
                val presenter = remember { ProfileModule.build(container, router) }
                DisposableEffect(presenter) { onDispose { presenter.interactor.dispose() } }
                ProfileScreen(presenter)
            }
        }
    }

    @Composable
    fun Destination(route: AppRoute, router: Router) {
        when (route) {
            is AppRoute.RecipeDetail -> {
                val presenter = remember(route.recipeID) { RecipeDetailModule.build(route.recipeID, container, router) }
                DisposableEffect(presenter) { onDispose { presenter.interactor.dispose() } }
                RecipeDetailScreen(presenter, onBack = router::pop)
            }
            is AppRoute.BrewTimer -> {
                val presenter = remember(route.recipeID) { BrewTimerModule.build(route.recipeID, container, router) }
                DisposableEffect(presenter) { onDispose { presenter.interactor.dispose() } }
                BrewTimerScreen(presenter)
            }
            is AppRoute.ProductDetail -> {
                val presenter = remember(route.productID) { ProductDetailModule.build(route.productID, container, router) }
                DisposableEffect(presenter) { onDispose { presenter.interactor.dispose() } }
                ProductDetailScreen(presenter, onBack = router::pop)
            }
            is AppRoute.Cart -> {
                val presenter = remember { CartModule.build(container, router) }
                DisposableEffect(presenter) { onDispose { presenter.interactor.dispose() } }
                CartScreen(presenter, onBack = router::pop)
            }
            is AppRoute.Checkout -> {
                val presenter = remember { CheckoutModule.build(container, router) }
                DisposableEffect(presenter) { onDispose { presenter.interactor.dispose() } }
                CheckoutScreen(presenter, onBack = router::pop)
            }
            is AppRoute.OrderStatus -> {
                val presenter = remember(route.orderID) { OrderStatusModule.build(route.orderID, container, router) }
                DisposableEffect(presenter) { onDispose { presenter.interactor.dispose() } }
                OrderStatusScreen(presenter)
            }
            is AppRoute.LoyaltyRedeem -> {
                val presenter = remember { RedeemModule.build(container) }
                DisposableEffect(presenter) { onDispose { presenter.interactor.dispose() } }
                RedeemScreen(presenter, onBack = router::pop)
            }
        }
    }
}

@Composable
private fun Placeholder(title: String, message: String) {
    Box(Modifier.fillMaxSize().background(Theme.Palette.noir), contentAlignment = Alignment.Center) {
        AppBackground()
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(title, style = SerafinoType.title, color = Theme.Palette.foam, textAlign = TextAlign.Center)
            Text(message, style = SerafinoType.subheadline, color = Theme.Palette.latte, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

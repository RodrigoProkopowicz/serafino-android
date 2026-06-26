package com.serafino.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.serafino.architecture.RootScreen
import com.serafino.architecture.Router
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme

private data class TabItem(val screen: RootScreen, val label: String, val icon: ImageVector)

/**
 * Las pestañas de nivel superior. Tienda y Perfil solo donde opera la tienda (Argentina); fuera
 * de AR queda Recetas (+ Ajustes). Espeja `RootTabView` de iOS.
 */
@Composable
fun RootTab(factory: ModuleFactory, showsStoreSections: Boolean) {
    val tabs = remember(showsStoreSections) {
        buildList {
            if (showsStoreSections) {
                add(TabItem(RootScreen.Profile, "Perfil", Icons.Filled.Person))
                add(TabItem(RootScreen.Store, "Tienda", Icons.Filled.ShoppingBag))
            }
            add(TabItem(RootScreen.Catalog, "Recetas", Icons.Filled.Coffee))
            add(TabItem(RootScreen.Settings, "Ajustes", Icons.Filled.Settings))
        }
    }
    var selected by rememberSaveable { mutableIntStateOf(if (showsStoreSections) tabs.indexOfFirst { it.screen == RootScreen.Catalog } else 0) }

    Scaffold(
        containerColor = Theme.Palette.noir,
        bottomBar = {
            NavigationBar(containerColor = Theme.Palette.noirDeep) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, style = SerafinoType.caption2) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Theme.Palette.gold,
                            selectedTextColor = Theme.Palette.gold,
                            indicatorColor = Color.White.copy(alpha = 0.08f),
                            unselectedIconColor = Theme.Palette.latte,
                            unselectedTextColor = Theme.Palette.latte,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
            NavigationFlow(root = tabs[selected].screen, factory = factory)
        }
    }
}

/** Un "stack" de navegación por pestaña, con su propio [Router]. Espeja `NavigationFlow` de iOS. */
@Composable
private fun NavigationFlow(root: RootScreen, factory: ModuleFactory) {
    val router = remember(root) { Router() }
    BackHandler(enabled = router.path.isNotEmpty()) { router.pop() }

    Box(Modifier.fillMaxSize()) {
        // La raíz queda viva (preserva estado); el destino se superpone a pantalla completa.
        factory.Root(root, router)
        val top = router.path.lastOrNull()
        if (top != null) {
            // El destino se superpone a la raíz, que sigue compuesta debajo (preserva su estado).
            // El overlay consume los toques de las zonas vacías para que NO se filtren a la raíz
            // (si no, tocar fuera de un control abriría/alternaría algo de la pantalla de atrás).
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                factory.Destination(top, router)
            }
        }
    }
}

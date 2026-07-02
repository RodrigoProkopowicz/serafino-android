package com.serafino.feature.store.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.AppBackground
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.components.CTAButton
import com.serafino.designsystem.components.EmptyStateView
import com.serafino.designsystem.glassCapsule
import com.serafino.designsystem.store.CartButton
import com.serafino.designsystem.store.CatalogSkeletonView
import com.serafino.designsystem.store.ProductCardModel
import com.serafino.designsystem.store.ProductImage
import com.serafino.designsystem.store.RoastFilterBar
import com.serafino.designsystem.store.SerafinoPullToRefresh
import com.serafino.designsystem.tv.CinematicHero
import com.serafino.designsystem.tv.ContentRail
import com.serafino.designsystem.tv.ProductPosterCard
import com.serafino.designsystem.tv.tvFocusClickable
import com.serafino.domain.entities.store.RoastFilter

/** Pantalla del catálogo de tienda (home Apple TV). Espeja `StoreCatalogView`/`StoreCatalogContentView`. */
@Composable
fun StoreCatalogScreen(presenter: StoreCatalogPresenter) {
    val interactor = presenter.interactor
    LaunchedEffect(Unit) { presenter.onAppear() }
    // Auto-refresco mientras la Tienda está visible; salir del tab cancela el bucle.
    LaunchedEffect(Unit) { presenter.autoRefresh() }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Theme.Palette.noir)) {
        AppBackground()

        // Pull-to-refresh nativo; el indicador baja para quedar visible sobre el hero,
        // debajo de la barra flotante del logo (este scroll sangra bajo el status bar).
        SerafinoPullToRefresh(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    presenter.refresh()
                    refreshing = false
                }
            },
            modifier = Modifier.fillMaxSize(),
            indicatorTopPadding = 52.dp,
        ) {
            when (val state = interactor.state) {
                StoreCatalogInteractor.State.Idle, StoreCatalogInteractor.State.Loading ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { CatalogSkeletonView() }
                StoreCatalogInteractor.State.Empty ->
                    Box(Modifier.fillMaxSize().padding(top = 80.dp, start = Theme.Spacing.lg, end = Theme.Spacing.lg), contentAlignment = Alignment.TopCenter) {
                        EmptyStateView(Icons.Filled.ShoppingBag, "Catálogo vacío", "No hay cafés disponibles por ahora. Volvé en un rato.")
                    }
                is StoreCatalogInteractor.State.Error ->
                    Column(
                        Modifier.fillMaxSize().padding(top = 80.dp, start = Theme.Spacing.lg, end = Theme.Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        EmptyStateView(Icons.Filled.WifiOff, "No se pudo cargar", state.message)
                        CTAButton("Reintentar", icon = Icons.Filled.Refresh, onClick = presenter::retry)
                    }
                StoreCatalogInteractor.State.Loaded ->
                    LoadedContent(interactor.data, scrollState, presenter::didSelect, presenter::selectRoast)
            }
        }

        // Barra superior flotante: wordmark (tap → scroll al inicio) + carrito.
        Row(
            Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = Theme.Spacing.lg, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(com.serafino.designsystem.R.drawable.serafino_wordmark),
                contentDescription = "Serafino",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .glassCapsule(interactive = false)
                    .clickable {
                        com.serafino.designsystem.Haptics.tap()
                        scope.launch { scrollState.animateScrollTo(0) }
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .height(20.dp),
            )
            Spacer(Modifier.weight(1f))
            CartButton(count = interactor.data.cartCount, onClick = presenter::openCart)
        }
    }
}

private data class Rail(val roast: RoastFilter, val items: List<ProductCardModel>)

private fun computeRails(selectedRoast: RoastFilter, products: List<ProductCardModel>): List<Rail> {
    if (selectedRoast != RoastFilter.All) {
        return if (products.isEmpty()) emptyList() else listOf(Rail(selectedRoast, products))
    }
    val order = listOf(RoastFilter.Light, RoastFilter.Medium, RoastFilter.Dark)
    val groups = order.mapNotNull { bucket ->
        val items = products.filter { RoastFilter.bucket(it.roast) == bucket }
        if (items.isEmpty()) null else Rail(bucket, items)
    }.toMutableList()
    val others = products.filter { RoastFilter.bucket(it.roast) == null }
    if (others.isNotEmpty()) groups.add(Rail(RoastFilter.All, others))
    return groups
}

private fun railTitle(roast: RoastFilter): String = when (roast) {
    RoastFilter.All -> "Más cafés"
    RoastFilter.Light -> "Tueste claro"
    RoastFilter.Medium -> "Tueste medio"
    RoastFilter.Dark -> "Tueste oscuro"
}

private fun railIcon(roast: RoastFilter): ImageVector = when (roast) {
    RoastFilter.All -> Icons.Filled.GridView
    RoastFilter.Light -> Icons.Filled.WbSunny
    RoastFilter.Medium -> Icons.Filled.Brightness6
    RoastFilter.Dark -> Icons.Filled.DarkMode
}

@Composable
private fun LoadedContent(
    data: StoreCatalogInteractor.Data,
    scrollState: androidx.compose.foundation.ScrollState,
    onSelect: (String) -> Unit,
    onSelectRoast: (RoastFilter) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xl),
    ) {
        val featured = data.featured
        if (featured != null) StoreHero(featured, onSelect) else Spacer(Modifier.height(96.dp))

        if (data.roastFilters.size > 1) {
            RoastFilterBar(filters = data.roastFilters, selected = data.selectedRoast, onSelect = onSelectRoast)
        }

        computeRails(data.selectedRoast, data.products).forEach { rail ->
            ContentRail(title = railTitle(rail.roast), icon = railIcon(rail.roast)) {
                items(rail.items, key = { it.id }) { product ->
                    Box(Modifier.tvFocusClickable { onSelect(product.id) }) {
                        ProductPosterCard(model = product)
                    }
                }
            }
        }
        Spacer(Modifier.height(Theme.Spacing.xl))
    }
}

@Composable
private fun StoreHero(featured: ProductCardModel, onSelect: (String) -> Unit) {
    CinematicHero(
        modifier = Modifier.clickable { onSelect(featured.id) },
        height = 460.dp,
        backdrop = { ProductImage(url = featured.imageURL, accent = featured.accent, modifier = Modifier.fillMaxSize()) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("CAFÉ DE LA SEMANA", style = SerafinoType.caption2, fontWeight = FontWeight.Bold, color = Theme.Palette.gold)
            Text(featured.name, style = SerafinoType.largeTitle, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2)
            if (featured.notesText.isNotEmpty()) {
                Text(featured.notesText, style = SerafinoType.subheadline, color = Color.White.copy(alpha = 0.82f), maxLines = 2)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.serafino.designsystem.store.PriceView(featured.pricing, prominent = true)
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.background(Theme.Palette.gold, RoundedCornerShape(percent = 50)).clickable { onSelect(featured.id) }.padding(horizontal = 18.dp, vertical = 11.dp),
                ) {
                    Text("Ver café", style = SerafinoType.subheadline, fontWeight = FontWeight.Bold, color = Theme.Palette.noir)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Theme.Palette.noir, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

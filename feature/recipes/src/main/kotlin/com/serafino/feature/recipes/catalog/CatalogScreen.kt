package com.serafino.feature.recipes.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.AppBackground
import com.serafino.designsystem.RecipeRowModel
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.accentOnDark
import com.serafino.designsystem.components.EmptyStateView
import com.serafino.designsystem.components.FavoriteHeartButton
import com.serafino.designsystem.glassCapsule
import com.serafino.designsystem.icon
import com.serafino.designsystem.tint
import com.serafino.designsystem.tv.CinematicHero
import com.serafino.designsystem.tv.ContentRail
import com.serafino.designsystem.tv.MethodBackdrop
import com.serafino.designsystem.tv.RecipePosterCard
import com.serafino.designsystem.tv.tvFocusClickable

/**
 * Pantalla del Catálogo: hero cinematográfico de la receta destacada + un riel por método, con
 * una barra flotante (buscador + favoritas). Espeja `CatalogView`/`CatalogContentView` de iOS.
 */
@Composable
fun CatalogScreen(presenter: CatalogPresenter) {
    val interactor = presenter.interactor
    LaunchedEffect(Unit) { presenter.onAppear() }

    Box(Modifier.fillMaxSize().background(Theme.Palette.noir)) {
        AppBackground()

        when (interactor.state) {
            CatalogInteractor.State.Idle, CatalogInteractor.State.Loading ->
                CatalogSkeleton(Modifier.padding(top = 96.dp))
            CatalogInteractor.State.Empty ->
                EmptyState(interactor.data, Modifier.padding(top = 140.dp))
            CatalogInteractor.State.Loaded ->
                LoadedContent(
                    data = interactor.data,
                    onSelect = presenter::didSelect,
                    onToggleFavorite = presenter::toggleFavorite,
                )
        }

        TopBar(
            searchText = interactor.data.searchText,
            showFavorites = interactor.data.showFavoritesOnly,
            onSearch = presenter::search,
            onToggleFavorites = presenter::toggleFavoritesFilter,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun LoadedContent(
    data: CatalogInteractor.Data,
    onSelect: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xl),
    ) {
        val hero = if (!data.showFavoritesOnly) data.sections.firstOrNull()?.items?.firstOrNull() else null
        if (hero != null) {
            RecipeHero(hero, onSelect)
        } else {
            Spacer(Modifier.height(96.dp))
        }

        data.sections.forEach { section ->
            ContentRail(
                title = section.method.displayName,
                accent = section.method.accentOnDark,
                icon = section.method.icon,
            ) {
                items(section.items, key = { it.id }) { item ->
                    Box {
                        Box(Modifier.tvFocusClickable { onSelect(item.id) }) {
                            RecipePosterCard(model = item)
                        }
                        FavoriteHeartButton(
                            isFavorite = item.isFavorite,
                            onClick = { onToggleFavorite(item.id) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Theme.Spacing.xl))
    }
}

@Composable
private fun RecipeHero(recipe: RecipeRowModel, onSelect: (String) -> Unit) {
    CinematicHero(
        modifier = Modifier.clickable { onSelect(recipe.id) },
        height = 440.dp,
        backdrop = { MethodBackdrop(method = recipe.method, icon = recipe.method.icon) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                recipe.method.displayName.uppercase(),
                style = SerafinoType.caption2,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f),
            )
            Text(
                recipe.name,
                style = SerafinoType.largeTitle,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                recipe.summary,
                style = SerafinoType.subheadline,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    recipe.difficulty.label,
                    style = SerafinoType.caption,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(recipe.difficulty.tint, RoundedCornerShape(percent = 50))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
                HeroLabel(Icons.Filled.SwapHoriz, recipe.ratioText)
                HeroLabel(Icons.Filled.Timer, recipe.timeText)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(percent = 50))
                    .clickable { onSelect(recipe.id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text("Ver receta", style = SerafinoType.subheadline, fontWeight = FontWeight.Bold, color = Theme.Palette.noir)
                Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Theme.Palette.noir, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun HeroLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(15.dp))
        Text(text, style = SerafinoType.subheadline, color = Color.White.copy(alpha = 0.9f))
    }
}

@Composable
private fun TopBar(
    searchText: String,
    showFavorites: Boolean,
    onSearch: (String) -> Unit,
    onToggleFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = Theme.Spacing.lg, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .glassCapsule(interactive = false)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Theme.Palette.latte, modifier = Modifier.size(18.dp))
            BasicTextField(
                value = searchText,
                onValueChange = onSearch,
                singleLine = true,
                textStyle = LocalTextStyle.current.merge(SerafinoType.subheadline).copy(color = Theme.Palette.foam),
                cursorBrush = SolidColor(Theme.Palette.caramel),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (searchText.isEmpty()) {
                        Text("Buscar recetas", style = SerafinoType.subheadline, color = Theme.Palette.latte.copy(alpha = 0.7f))
                    }
                    inner()
                },
            )
            if (searchText.isNotEmpty()) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Borrar búsqueda",
                    tint = Theme.Palette.latte,
                    modifier = Modifier.size(18.dp).clickable { onSearch("") },
                )
            }
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .glassCapsule()
                .clickable(onClick = onToggleFavorites),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (showFavorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (showFavorites) "Mostrar todas" else "Mostrar favoritas",
                tint = if (showFavorites) Theme.Palette.berry else Theme.Palette.latte,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun EmptyState(data: CatalogInteractor.Data, modifier: Modifier = Modifier) {
    val (icon, title, message) = when {
        data.showFavoritesOnly && !data.hasFavorites -> Triple(
            Icons.Filled.FavoriteBorder,
            "Todavía no tenés favoritas",
            "Tocá el corazón en una receta para guardarla y verla acá.",
        )
        data.showFavoritesOnly -> Triple(
            Icons.Filled.FavoriteBorder,
            "Sin coincidencias",
            "Ninguna de tus recetas favoritas coincide con la búsqueda.",
        )
        else -> Triple(
            Icons.Filled.Search,
            "Sin resultados",
            "Probá con otro método o nota de sabor.",
        )
    }
    Box(modifier.fillMaxSize().padding(horizontal = Theme.Spacing.lg), contentAlignment = Alignment.TopCenter) {
        EmptyStateView(icon = icon, title = title, message = message)
    }
}

@Composable
private fun CatalogSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(Theme.Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
    ) {
        repeat(4) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .glassCapsule(interactive = false),
            )
        }
        Text(
            "Cargando recetas…",
            style = SerafinoType.footnote,
            color = Theme.Palette.latte,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

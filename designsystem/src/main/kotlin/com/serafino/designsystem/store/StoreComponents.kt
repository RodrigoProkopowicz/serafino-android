package com.serafino.designsystem.store

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.glassCapsule
import com.serafino.domain.entities.store.Pricing
import com.serafino.domain.entities.store.RoastFilter
import com.serafino.domain.services.Money

/**
 * Precio en dos líneas cuando hay promo: el de lista tachado arriba (chico, secundario) y el
 * efectivo abajo (prominente), con el % de descuento al costado. Espeja `PriceView` de iOS.
 */
@Composable
fun PriceView(pricing: Pricing, modifier: Modifier = Modifier, prominent: Boolean = false) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (pricing.active) {
                Text(
                    Money.ars(pricing.original),
                    style = if (prominent) SerafinoType.subheadline else SerafinoType.caption,
                    color = Theme.Palette.latte,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                )
            }
            Text(
                Money.ars(pricing.price),
                style = if (prominent) SerafinoType.title2 else SerafinoType.headline,
                fontWeight = FontWeight.Bold,
                color = Theme.Palette.foam,
            )
        }
        if (pricing.active) {
            Text(
                "-${pricing.discountPct}%",
                style = SerafinoType.caption2,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.background(Theme.Palette.caramel, RoundedCornerShape(percent = 50)).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** Medidor de intensidad (0–10). Espeja `IntensityMeter`. */
@Composable
fun IntensityMeter(value: Double, modifier: Modifier = Modifier, tint: Color = Theme.Palette.caramel) {
    val fraction = (value / 10).coerceIn(0.0, 1.0).toFloat()
    val formatted = if (value == value.toInt().toDouble()) value.toInt().toString()
    else "%.1f".format(value).replace(".", ",")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            Text("Intensidad", style = SerafinoType.caption, fontWeight = FontWeight.SemiBold, color = Theme.Palette.latte)
            Box(Modifier.weight(1f))
            Text("$formatted/10", style = SerafinoType.caption, fontWeight = FontWeight.Bold, color = Theme.Palette.foam)
        }
        Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(percent = 50)).background(Color.White.copy(alpha = 0.08f))) {
            Box(Modifier.fillMaxWidth(fraction).height(7.dp).clip(RoundedCornerShape(percent = 50)).background(Brush.horizontalGradient(listOf(tint.copy(alpha = 0.85f), tint))))
        }
    }
}

/** Nivel de tueste en 5 puntos (claro → oscuro). Espeja `RoastMeter`. */
@Composable
fun RoastMeter(roast: String, modifier: Modifier = Modifier, tint: Color = Theme.Palette.gold) {
    val level = run {
        val r = roast.lowercase()
        when {
            r.contains("oscur") -> if (r.contains("medi")) 4 else 5
            r.contains("clar") -> if (r.contains("medi")) 2 else 1
            else -> 3
        }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tueste", style = SerafinoType.caption, fontWeight = FontWeight.SemiBold, color = Theme.Palette.foam.copy(alpha = 0.72f))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (1..5).forEach { i ->
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(percent = 50)).background(if (i <= level) tint else Theme.Palette.foam.copy(alpha = 0.18f)))
            }
        }
    }
}

/** Fila de chips para filtrar por tueste. Espeja `RoastFilterBar`. */
@Composable
fun RoastFilterBar(
    filters: List<RoastFilter>,
    selected: RoastFilter,
    onSelect: (RoastFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Theme.Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        filters.forEach { filter ->
            val isOn = filter == selected
            val base = Modifier.clip(RoundedCornerShape(percent = 50)).clickable { onSelect(filter) }
            val styled = if (isOn) base.background(Theme.Palette.gold) else base.glassCapsule(interactive = false)
            Text(
                filter.label,
                style = SerafinoType.subheadline,
                fontWeight = FontWeight.SemiBold,
                color = if (isOn) Theme.Palette.noir else Color.White.copy(alpha = 0.92f),
                modifier = styled.padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

/** − / cantidad / + en una cápsula glass. Espeja `QuantityStepper`. */
@Composable
fun QuantityStepper(
    quantity: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.glassCapsule(interactive = false).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StepButton(Icons.Filled.Remove, onDecrement)
        Text("$quantity", style = SerafinoType.headline, fontFamily = FontFamily.Monospace, color = Theme.Palette.foam, modifier = Modifier.widthIn(min = 22.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        StepButton(Icons.Filled.Add, onIncrement)
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(26.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Theme.Palette.foam, modifier = Modifier.size(18.dp))
    }
}

/** Botón "bolsa" del carrito con contador integrado. Espeja `CartButton`/`CartBadge`. */
@Composable
fun CartButton(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .widthIn(min = 46.dp)
            .height(45.dp)
            .glassCapsule()
            .clickable(onClick = onClick)
            .padding(horizontal = if (count > 0) 11.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(Icons.Outlined.ShoppingBag, contentDescription = "Carrito, $count artículos", tint = Theme.Palette.foam, modifier = Modifier.size(22.dp))
        if (count > 0) {
            Text(if (count > 99) "99+" else "$count", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Theme.Palette.foam)
        }
    }
}

/** Bloque skeleton con shimmer. */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, cornerRadius: androidx.compose.ui.unit.Dp = 7.dp) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    Box(modifier.clip(RoundedCornerShape(cornerRadius)).background(Color.White.copy(alpha = alpha)))
}

/** Catálogo en carga: hero a sangre + un par de rieles. Espeja `CatalogSkeletonView`. */
@Composable
fun CatalogSkeletonView(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xl)) {
        SkeletonBlock(Modifier.fillMaxWidth().height(420.dp), cornerRadius = 0.dp)
        repeat(2) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBlock(Modifier.width(150.dp).height(20.dp).padding(start = Theme.Spacing.md))
                Row(
                    Modifier.padding(horizontal = Theme.Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
                ) {
                    repeat(3) { SkeletonBlock(Modifier.width(158.dp).height(250.dp), cornerRadius = Theme.Radius.poster) }
                }
            }
        }
    }
}

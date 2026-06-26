package com.serafino.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.glassCard

/** Contenedor glass con padding consistente. Espeja `GlassCard` de iOS. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Theme.Radius.card,
    tint: Color? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .glassCard(cornerRadius = cornerRadius, tint = tint)
            .padding(Theme.Spacing.md),
    ) {
        content()
    }
}

/** Tarjeta glass con un dato clave de la receta. Espeja `MetricBadge` de iOS. */
@Composable
fun MetricBadge(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = Theme.Palette.caramel,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .glassCard(cornerRadius = 20.dp)
            .padding(vertical = 14.dp, horizontal = 8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(
            value,
            style = SerafinoType.headline,
            color = Theme.Palette.foam,
            maxLines = 1,
        )
        Text(
            label.uppercase(),
            style = SerafinoType.caption2,
            fontWeight = FontWeight.SemiBold,
            color = Theme.Palette.latte,
        )
    }
}

/** Estado vacío reutilizable dentro de una tarjeta glass. Espeja `EmptyStateView` de iOS. */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .glassCard()
            .padding(28.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Theme.Palette.caramel, modifier = Modifier.size(44.dp))
        Text(title, style = SerafinoType.title3, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Theme.Palette.foam)
        Text(message, style = SerafinoType.subheadline, color = Theme.Palette.latte, textAlign = TextAlign.Center)
    }
}

/**
 * Tarjeta glass tipo acordeón: ícono + título y un chevron que rota. Al tocar la cabecera
 * revela/oculta su contenido en el mismo lugar. Espeja `DisclosureCard` de iOS.
 */
@Composable
fun DisclosureCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Column(
        modifier = modifier
            .glassCard()
            .padding(Theme.Spacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
        ) {
            Icon(icon, contentDescription = null, tint = Theme.Palette.gold, modifier = Modifier.width(30.dp).size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = SerafinoType.headline, color = Theme.Palette.foam)
                if (subtitle != null) Text(subtitle, style = SerafinoType.caption, color = Theme.Palette.latte)
            }
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = Theme.Palette.latte,
                modifier = Modifier.rotate(rotation),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.fillMaxWidth().padding(top = Theme.Spacing.md)) { content() }
        }
    }
}

/**
 * Fila glass que actúa como botón para abrir más contenido: ícono + título (+ subtítulo) y un
 * chevron a la derecha. Espeja `DisclosureRow` de iOS.
 */
@Composable
fun DisclosureRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .glassCard()
            .clickable(onClick = onClick)
            .padding(Theme.Spacing.md),
    ) {
        Icon(icon, contentDescription = null, tint = Theme.Palette.gold, modifier = Modifier.width(30.dp).size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = SerafinoType.headline, color = Theme.Palette.foam)
            if (subtitle != null) Text(subtitle, style = SerafinoType.caption, color = Theme.Palette.latte)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Theme.Palette.latte)
    }
}

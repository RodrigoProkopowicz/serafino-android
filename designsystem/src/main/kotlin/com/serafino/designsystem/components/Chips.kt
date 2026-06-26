package com.serafino.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.glassCapsule
import com.serafino.designsystem.gradient
import com.serafino.domain.entities.BrewMethod

/**
 * Chip glass compacto para etiquetas (notas de sabor, método, dificultad).
 * Espeja `TagChip` de iOS.
 */
@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = Theme.Palette.mocha,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.glassCapsule(interactive = false).padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
        Text(text, style = SerafinoType.caption, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

/** Cabecera de sección consistente. Espeja `SectionLabel` de iOS. */
@Composable
fun SectionLabel(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = Theme.Palette.gold, modifier = Modifier.size(20.dp))
        Text(title, style = SerafinoType.title3, fontWeight = FontWeight.Bold, color = Theme.Palette.foam)
    }
}

/** Ícono del método dentro de una pastilla con degradado de su color. Espeja `MethodGlyph`. */
@Composable
fun MethodGlyph(
    method: BrewMethod,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(method.gradient),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.42f),
        )
    }
}

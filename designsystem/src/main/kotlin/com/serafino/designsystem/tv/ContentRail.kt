package com.serafino.designsystem.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme

/**
 * El "shelf" de Apple TV: una cabecera grande y una fila de posters que scrollea de lado.
 * Espeja `ContentRail` de iOS.
 */
@Composable
fun ContentRail(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accent: Color = Theme.Palette.gold,
    icon: ImageVector? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.padding(horizontal = Theme.Spacing.md),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(title, style = SerafinoType.title3, fontWeight = FontWeight.Bold, color = Color.White)
            if (subtitle != null) {
                Text(subtitle, style = SerafinoType.footnote, color = Color.White.copy(alpha = 0.45f))
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
            contentPadding = PaddingValues(horizontal = Theme.Spacing.md, vertical = 12.dp),
            content = content,
        )
    }
}

package com.serafino.designsystem.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.RecipeRowModel
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.icon

/**
 * Poster de receta para los rieles de Recetas: el arte del método a sangre, con scrim para
 * asentar el texto — un poster apaisado tipo "show" de Apple TV. Espeja `RecipePosterCard` de iOS.
 */
@Composable
fun RecipePosterCard(
    model: RecipeRowModel,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 248.dp,
    height: androidx.compose.ui.unit.Dp = 158.dp,
) {
    val shape = RoundedCornerShape(Theme.Radius.poster)
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape),
        contentAlignment = Alignment.BottomStart,
    ) {
        MethodBackdrop(method = model.method, icon = model.method.icon)
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.72f))
                    )
                )
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            Text(
                model.method.shortName.uppercase(),
                style = SerafinoType.caption2,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f),
            )
            Text(
                model.name,
                style = SerafinoType.title3,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PosterLabel(Icons.Filled.SwapHoriz, model.ratioText)
                PosterLabel(Icons.Filled.Timer, model.timeText)
            }
        }
    }
}

@Composable
private fun PosterLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.size(13.dp))
        Text(text, style = SerafinoType.caption, color = Color.White.copy(alpha = 0.82f))
    }
}

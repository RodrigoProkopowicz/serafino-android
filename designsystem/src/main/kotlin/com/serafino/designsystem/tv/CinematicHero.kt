package com.serafino.designsystem.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.Theme

/**
 * El banner de portada estilo Apple TV: arte a sangre, un scrim que funde la imagen en el negro
 * de la página y un bloque de contenido abajo-izquierda. Espeja `CinematicHero` de iOS.
 */
@Composable
fun CinematicHero(
    modifier: Modifier = Modifier,
    height: Dp = 460.dp,
    backdrop: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.BottomStart,
    ) {
        backdrop()
        // Scrim: oscurece el tope (legibilidad) y funde el hero en el negro de la página.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Theme.Palette.noir.copy(alpha = 0.28f),
                        0.18f to Color.Transparent,
                        0.46f to Theme.Palette.noir.copy(alpha = 0.15f),
                        0.73f to Theme.Palette.noir.copy(alpha = 0.74f),
                        1.0f to Theme.Palette.noir,
                    )
                )
        )
        Box(
            Modifier.padding(horizontal = Theme.Spacing.lg, vertical = Theme.Spacing.lg),
        ) { content() }
    }
}

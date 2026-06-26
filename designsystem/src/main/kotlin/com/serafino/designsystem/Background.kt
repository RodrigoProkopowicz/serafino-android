package com.serafino.designsystem

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Fondo cine de toda la app: negro profundo con glows café ambientales que le dan al glass algo
 * rico que refractar. Dark-first. Espeja `AppBackground` de iOS.
 */
fun Modifier.appBackground(): Modifier = this.drawBehind {
    // Base vertical noir → noirDeep.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Theme.Palette.noir, Theme.Palette.noirDeep),
            startY = 0f,
            endY = size.height,
        )
    )
    // Glow caramelo arriba-derecha: la "luz" que tiñe la escena.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Theme.Palette.caramel.copy(alpha = 0.30f), Color.Transparent),
            center = Offset(size.width * 0.85f, size.height * 0.02f),
            radius = maxOf(size.minDimension, 1f) * 0.95f,
        )
    )
    // Glow mocha frío abajo-izquierda para dar profundidad.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Theme.Palette.mocha.copy(alpha = 0.40f), Color.Transparent),
            center = Offset(0f, size.height),
            radius = maxOf(size.minDimension, 1f) * 0.85f,
        )
    )
    // Vignette sutil para asentar el contenido (look de sala de cine).
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Theme.Palette.noirDeep.copy(alpha = 0.55f)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = maxOf(size.maxDimension, 1f) * 0.75f,
        )
    )
}

/** Capa de fondo a pantalla completa (úsese como primera capa de un `Box`). */
@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Spacer(
        modifier = modifier.fillMaxSize().appBackground()
    )
}

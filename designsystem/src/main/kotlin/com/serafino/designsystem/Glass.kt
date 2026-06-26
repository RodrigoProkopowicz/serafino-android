package com.serafino.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * El único lugar que conoce el estilo "Liquid Glass". En iOS 26 es `glassEffect` real; acá se
 * aproxima con Material 3 sobre el fondo cine: una superficie translúcida tintada con un borde
 * fino. Como todo el branching vive acá, los composables de feature no piensan en esto.
 * Espeja `GlassStyle` de iOS.
 */
fun Modifier.liquidGlass(
    shape: Shape,
    tint: Color? = null,
    @Suppress("UNUSED_PARAMETER") interactive: Boolean = false,
): Modifier {
    var m = this
        .clip(shape)
        .background(Color.White.copy(alpha = 0.06f), shape)
    if (tint != null) {
        m = m.background(tint.copy(alpha = 0.16f), shape)
    }
    return m.border(0.8.dp, Color.White.copy(alpha = 0.12f), shape)
}

/** Superficie tipo tarjeta con esquinas redondeadas. */
fun Modifier.glassCard(cornerRadius: Dp = Theme.Radius.card, tint: Color? = null): Modifier =
    liquidGlass(RoundedCornerShape(cornerRadius), tint = tint)

/** Cápsula glass (chips, botones redondos). */
fun Modifier.glassCapsule(tint: Color? = null, interactive: Boolean = true): Modifier =
    liquidGlass(RoundedCornerShape(percent = 50), tint = tint, interactive = interactive)

/**
 * Agrupa varias superficies glass. En Compose es un simple contenedor pass-through (el morphing
 * de iOS 26 no aplica). Espeja `GlassContainer` de iOS.
 */
@Composable
fun GlassContainer(content: @Composable () -> Unit) {
    content()
}

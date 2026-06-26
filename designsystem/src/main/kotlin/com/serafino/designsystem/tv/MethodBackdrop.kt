package com.serafino.designsystem.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.gradient
import com.serafino.domain.entities.BrewMethod
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable

/**
 * Arte de portada de un método. En iOS es una foto de la cafetera; acá, como no contamos con
 * esos binarios, se usa el degradado de marca del método + un glifo tenue como stand-in fiel a
 * la estética cinematográfica.
 */
@Composable
fun MethodBackdrop(
    method: BrewMethod,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(method.gradient),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.18f),
            modifier = Modifier
                .fillMaxSize(0.7f)
                .rotate(-12f)
                .scale(1.2f),
        )
    }
}

/**
 * "Focus" táctil estilo Apple TV: al presionar, el poster crece levemente. Espeja `TVCardStyle`.
 */
@Composable
fun Modifier.tvFocusClickable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 1.06f else 1f, label = "tvFocus")
    return this
        .scale(scale)
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

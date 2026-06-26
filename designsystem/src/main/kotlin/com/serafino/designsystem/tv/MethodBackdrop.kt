package com.serafino.designsystem.tv

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.serafino.designsystem.gradient
import com.serafino.designsystem.imageRes
import com.serafino.domain.entities.BrewMethod
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable

/**
 * Arte de portada de un método: la foto de la cafetera a sangre (mismos binarios que iOS), sobre
 * el degradado de marca del método como base/fallback hasta que decodifica la imagen. El parámetro
 * `icon` se conserva por compatibilidad con los call sites (ya no se dibuja el glifo stand-in).
 */
@Composable
fun MethodBackdrop(
    method: BrewMethod,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(method.gradient)) {
        Image(
            painter = painterResource(method.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
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

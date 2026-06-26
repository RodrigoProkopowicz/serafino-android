package com.serafino.designsystem.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.serafino.designsystem.accentGradient

/**
 * Imagen de producto con placeholder de degradado de marca (los `accent` del café) mientras carga
 * o si falla. Llena el frame del contenedor. Espeja `ProductImage`/`CachedAsyncImage` de iOS.
 */
@Composable
fun ProductImage(
    url: String?,
    accent: List<String>,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(accentGradient(accent)), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.LocalCafe, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.fillMaxSize(0.3f))
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

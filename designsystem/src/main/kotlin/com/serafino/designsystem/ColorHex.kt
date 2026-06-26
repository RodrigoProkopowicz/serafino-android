package com.serafino.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Crea un [Color] desde un HEX (`#RRGGBB` o `RRGGBB`). Devuelve `null` si no parsea.
 * Espeja `Color(hex:)` de iOS.
 */
fun colorFromHex(hex: String): Color? {
    var s = hex.trim()
    if (s.startsWith("#")) s = s.substring(1)
    if (s.length != 6) return null
    val value = s.toLongOrNull(16) ?: return null
    return Color(
        red = ((value shr 16) and 0xFF) / 255f,
        green = ((value shr 8) and 0xFF) / 255f,
        blue = (value and 0xFF) / 255f,
        alpha = 1f,
    )
}

/**
 * Degradado de marca a partir de los dos HEX `accent` de un producto. Cae a la paleta
 * caramelo→mocha si faltan o no parsean. Espeja `Color.accentGradient` de iOS.
 */
fun accentGradient(hex: List<String>): Brush {
    val colors = hex.mapNotNull { colorFromHex(it) }
    val resolved = if (colors.size >= 2) colors else listOf(Theme.Palette.caramel, Theme.Palette.mocha)
    return Brush.linearGradient(
        colors = resolved,
        start = Offset(0f, 0f),
        end = Offset.Infinite,
    )
}

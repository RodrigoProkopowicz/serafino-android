package com.serafino.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Tema Material 3 de la app. Dark-first (estilo cine Apple TV): esquema oscuro con acentos
 * caramelo sobre el noir. Espeja el modo dark forzado de la app iOS.
 */
private val SerafinoColorScheme = darkColorScheme(
    primary = Theme.Palette.gold,
    onPrimary = Theme.Palette.espresso,
    primaryContainer = Theme.Palette.mocha,
    onPrimaryContainer = Theme.Palette.foam,
    secondary = Theme.Palette.caramel,
    onSecondary = Theme.Palette.espresso,
    tertiary = Theme.Palette.latte,
    background = Theme.Palette.noir,
    onBackground = Theme.Palette.foam,
    surface = Theme.Palette.noir,
    onSurface = Theme.Palette.foam,
    surfaceVariant = Theme.Palette.espresso,
    onSurfaceVariant = Theme.Palette.latte,
    error = Theme.Palette.berry,
    onError = Color.White,
    outline = Color.White.copy(alpha = 0.12f),
)

private val SerafinoTypography = Typography(
    titleLarge = SerafinoType.title,
    titleMedium = SerafinoType.title2,
    titleSmall = SerafinoType.title3,
    headlineSmall = SerafinoType.headline,
    bodyLarge = SerafinoType.body,
    bodyMedium = SerafinoType.callout,
    bodySmall = SerafinoType.subheadline,
    labelLarge = SerafinoType.headline,
    labelMedium = SerafinoType.footnote,
    labelSmall = SerafinoType.caption,
)

@Composable
fun SerafinoTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // La app es dark-first: siempre el esquema cine, sin importar el sistema.
    MaterialTheme(
        colorScheme = SerafinoColorScheme,
        typography = SerafinoTypography,
        content = content,
    )
}

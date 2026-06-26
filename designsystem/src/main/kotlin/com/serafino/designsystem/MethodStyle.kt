package com.serafino.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.serafino.domain.entities.BrewMethod
import com.serafino.domain.entities.Difficulty

/**
 * Estilo de presentación para los enums de dominio. Se mantiene fuera de las entidades para que
 * el dominio quede libre de UI. Espeja `MethodStyle` de iOS.
 */

val BrewMethod.accentColor: Color
    get() = when (this) {
        BrewMethod.Espresso -> Color(red = 0.55f, green = 0.27f, blue = 0.13f)
        BrewMethod.PourOverV60 -> Color(red = 0.85f, green = 0.45f, blue = 0.20f)
        BrewMethod.Aeropress -> Color(red = 0.18f, green = 0.49f, blue = 0.55f)
        BrewMethod.FrenchPress -> Color(red = 0.45f, green = 0.32f, blue = 0.56f)
        BrewMethod.MokaPot -> Color(red = 0.78f, green = 0.30f, blue = 0.18f)
        BrewMethod.ColdBrew -> Color(red = 0.22f, green = 0.52f, blue = 0.78f)
        BrewMethod.Chemex -> Color(red = 0.60f, green = 0.45f, blue = 0.30f)
    }

/** Acento legible sobre el cine oscuro (íconos y labels chicos). */
val BrewMethod.accentOnDark: Color
    get() = when (this) {
        BrewMethod.Espresso -> Color(red = 0.80f, green = 0.52f, blue = 0.32f)
        BrewMethod.PourOverV60 -> Color(red = 0.95f, green = 0.58f, blue = 0.30f)
        BrewMethod.Aeropress -> Color(red = 0.40f, green = 0.74f, blue = 0.80f)
        BrewMethod.FrenchPress -> Color(red = 0.72f, green = 0.58f, blue = 0.86f)
        BrewMethod.MokaPot -> Color(red = 0.93f, green = 0.49f, blue = 0.35f)
        BrewMethod.ColdBrew -> Color(red = 0.46f, green = 0.72f, blue = 0.95f)
        BrewMethod.Chemex -> Color(red = 0.87f, green = 0.71f, blue = 0.49f)
    }

/** Degradado de marca del método (para posters y heroes). */
val BrewMethod.gradient: Brush
    get() = Brush.linearGradient(
        colors = listOf(accentColor.copy(alpha = 0.95f), accentColor.copy(alpha = 0.55f)),
        start = Offset(0f, 0f),
        end = Offset.Infinite,
    )

val Difficulty.tint: Color
    get() = when (this) {
        Difficulty.Easy -> Color(red = 0.30f, green = 0.62f, blue = 0.36f)
        Difficulty.Medium -> Color(red = 0.86f, green = 0.60f, blue = 0.20f)
        Difficulty.Hard -> Color(red = 0.80f, green = 0.32f, blue = 0.26f)
    }

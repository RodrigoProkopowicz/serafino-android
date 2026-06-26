package com.serafino.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tipografía de marca, espejando CoffeeStore: display serif (Fraunces) + cuerpo sans (Inter).
 * Acá se usan las familias del sistema (serif/sans-serif) como análogo inmediato; para fidelidad
 * total se pueden enchufar los TTF reales en `res/font` y reemplazar [displayFamily]/[bodyFamily].
 * Espeja `Typography` de iOS.
 */
object SerafinoType {
    // Punto único para enchufar las fuentes reales más adelante.
    val displayFamily: FontFamily = FontFamily.Serif
    val bodyFamily: FontFamily = FontFamily.SansSerif

    // Display — serif (los títulos se usan en bold/semibold en el call site).
    val largeTitle = TextStyle(fontFamily = displayFamily, fontSize = 34.sp, fontWeight = FontWeight.Bold)
    val title = TextStyle(fontFamily = displayFamily, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    val title2 = TextStyle(fontFamily = displayFamily, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    val title3 = TextStyle(fontFamily = displayFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

    // Cuerpo / UI — sans.
    val headline = TextStyle(fontFamily = bodyFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontFamily = bodyFamily, fontSize = 17.sp)
    val callout = TextStyle(fontFamily = bodyFamily, fontSize = 16.sp)
    val subheadline = TextStyle(fontFamily = bodyFamily, fontSize = 15.sp)
    val footnote = TextStyle(fontFamily = bodyFamily, fontSize = 13.sp)
    val caption = TextStyle(fontFamily = bodyFamily, fontSize = 12.sp)
    val caption2 = TextStyle(fontFamily = bodyFamily, fontSize = 11.sp)
}

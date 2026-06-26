package com.serafino.designsystem.store

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.Theme
import kotlin.math.roundToInt

/**
 * Banderas de países de origen del café, replicando los SVG de la web (`Flag.jsx`) y el
 * `Flag.swift` de iOS: formas simplificadas (sin escudos ni estrellas) para leerse a ~18 px.
 * Proporción 3:2 con esquinas redondeadas y aro sutil.
 */
@Composable
fun Flag(country: String, width: Dp = 18.dp) {
    val height = width * 2 / 3
    Canvas(
        Modifier
            .size(width, height)
            .clip(RoundedCornerShape(3.dp))
            .border(0.5.dp, Theme.Palette.foam.copy(alpha = 0.28f), RoundedCornerShape(3.dp)),
    ) {
        val w = size.width
        val h = size.height

        fun hStripes(stripes: List<Pair<Color, Float>>) {
            var y = 0f
            stripes.forEach { (c, frac) ->
                val sh = h * frac
                drawRect(c, topLeft = Offset(0f, y), size = Size(w, sh))
                y += sh
            }
        }

        fun vStripes(stripes: List<Pair<Color, Float>>) {
            var x = 0f
            stripes.forEach { (c, frac) ->
                val sw = w * frac
                drawRect(c, topLeft = Offset(x, 0f), size = Size(sw, h))
                x += sw
            }
        }

        fun disc(c: Color, diameter: Float) = drawCircle(c, radius = diameter / 2f, center = center)

        val third = 1f / 3f
        val sixth = 1f / 6f
        when (country) {
            "colombia" -> hStripes(listOf(hex("FCD116") to 0.5f, hex("003893") to 0.25f, hex("CE1126") to 0.25f))
            "bolivia" -> hStripes(listOf(hex("D52B1E") to third, hex("F9E300") to third, hex("007934") to third))
            "honduras" -> hStripes(listOf(hex("0073CF") to third, hex("FFFFFF") to third, hex("0073CF") to third))
            "kenia" -> hStripes(
                listOf(hex("000000") to 0.3f, hex("FFFFFF") to 0.05f, hex("BB0000") to 0.3f, hex("FFFFFF") to 0.05f, hex("006600") to 0.3f),
            )
            "costarica" -> hStripes(
                listOf(hex("002B7F") to sixth, hex("FFFFFF") to sixth, hex("CE1126") to third, hex("FFFFFF") to sixth, hex("002B7F") to sixth),
            )
            "peru" -> vStripes(listOf(hex("D91023") to third, hex("FFFFFF") to third, hex("D91023") to third))
            "guatemala" -> vStripes(listOf(hex("4997D0") to third, hex("FFFFFF") to third, hex("4997D0") to third))
            "mexico" -> vStripes(listOf(hex("006847") to third, hex("FFFFFF") to third, hex("CE1126") to third))
            "etiopia" -> {
                hStripes(listOf(hex("078930") to third, hex("FCDD09") to third, hex("DA121A") to third))
                disc(hex("0F47AF"), 0.42f * h)
            }
            "brasil" -> {
                drawRect(hex("009B3A"), size = Size(w, h))
                val rhombus = Path().apply {
                    moveTo(w * 0.5f, h * 0.09f)
                    lineTo(w * 0.94f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.91f)
                    lineTo(w * 0.06f, h * 0.5f)
                    close()
                }
                drawPath(rhombus, hex("FEDF00"))
                disc(hex("002776"), 0.36f * h)
            }
            else -> {}
        }
    }
}

private fun hex(s: String): Color = Color(("FF$s").toLong(16))

// MARK: - Detección de país en el texto de origen

/** Nombres (con/sin tilde o en inglés) → clave de bandera. Espeja COUNTRY_NAMES de la web. */
private val countryNames: List<Pair<String, String>> = listOf(
    "colombia" to "colombia",
    "brasil" to "brasil", "brazil" to "brasil",
    "perú" to "peru", "peru" to "peru",
    "bolivia" to "bolivia",
    "etiopía" to "etiopia", "etiopia" to "etiopia", "ethiopia" to "etiopia",
    "kenia" to "kenia", "kenya" to "kenia",
    "guatemala" to "guatemala",
    "honduras" to "honduras",
    "costa rica" to "costarica",
    "méxico" to "mexico", "mexico" to "mexico",
)

/**
 * Países detectados en el texto de origen (ej. "Brasil + Colombia"), en orden de aparición y sin
 * repetir. Espeja `originFlagKeys` de la web/iOS.
 */
fun originFlagKeys(text: String): List<String> {
    val t = text.lowercase()
    val hits = countryNames.mapNotNull { (name, key) ->
        val idx = t.indexOf(name)
        if (idx >= 0) key to idx else null
    }.sortedBy { it.second }.map { it.first }
    val seen = LinkedHashSet<String>()
    seen.addAll(hits)
    return seen.toList()
}

/**
 * Si el texto EMPIEZA con un país conocido (ej. "Brasil: mayo – septiembre"), devuelve su bandera,
 * el nombre tal como vino y el resto sin el ":". Espeja `splitCountryPrefix` de la web/iOS.
 */
fun splitCountryPrefix(text: String): Triple<String, String, String>? {
    val t = text.lowercase()
    for ((name, key) in countryNames) {
        if (t.startsWith(name)) {
            val label = text.take(name.length)
            val rest = text.drop(name.length).replace(Regex("^\\s*:\\s*"), "")
            return Triple(key, label, rest)
        }
    }
    return null
}

@Suppress("unused")
private fun Float.roundedPx(): Int = this.roundToInt()

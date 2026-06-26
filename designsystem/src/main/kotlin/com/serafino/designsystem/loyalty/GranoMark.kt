package com.serafino.designsystem.loyalty

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.Theme
import kotlin.math.max

/**
 * El "ícono moneda" del programa de fidelidad: un grano de café dibujado a mano (óvalo con
 * hendidura central en S). Tinte configurable. Espeja `GranoMark` de iOS.
 */
@Composable
fun GranoMark(modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color = Theme.Palette.gold) {
    Canvas(modifier.size(size).rotate(-26f)) {
        val w = this.size.width
        val h = this.size.height
        // Cuerpo del grano: óvalo algo más angosto que el marco.
        val bodyRect = Rect(
            offset = Offset(w * 0.13f, h * 0.02f),
            size = Size(w - w * 0.26f, h - h * 0.04f),
        )
        val body = Path().apply { addOval(bodyRect) }
        drawPath(body, brush = Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.78f))))
        drawPath(body, color = Color.Black.copy(alpha = 0.10f), style = Stroke(width = max(0.5f, w * 0.018f)))

        // Hendidura central en S.
        val crease = Path().apply {
            moveTo(w / 2f, h * 0.17f)
            cubicTo(
                w * 0.21f, h * 0.40f,
                w - w * 0.21f, h - h * 0.40f,
                w / 2f, h - h * 0.17f,
            )
        }
        drawPath(crease, color = Color.Black.copy(alpha = 0.32f), style = Stroke(width = max(1f, w * 0.075f), cap = StrokeCap.Round))
    }
}

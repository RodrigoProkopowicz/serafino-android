package com.serafino.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Paleta de marca, escala de espaciado y radios. Paleta "café con leche" del proyecto
 * CoffeeStore. Espeja `Theme` de iOS. La app es dark-first (cine estilo Apple TV).
 */
object Theme {
    object Palette {
        val espresso = Color(0xFF241610)
        val mocha = Color(0xFF6F4A2F)
        val caramel = Color(0xFFC0833F) // acento principal
        val latte = Color(0xFFD8B488)
        val foam = Color(0xFFF8F1E7)

        val clayDeep = Color(0xFFA3692C)
        val cream = Color(0xFFF3E7D4)
        val line = Color(0xFFD8C3A4)
        val green = Color(0xFF5F6B35)
        val berry = Color(0xFFA23C2E)

        // Cine oscuro (estilo Apple TV)
        val noir = Color(0xFF0B0807)
        val noirDeep = Color(0xFF050302)
        val gold = Color(0xFFE0A766)
    }

    object Spacing {
        val sm = 8.dp
        val md = 16.dp
        val lg = 24.dp
        val xl = 32.dp
    }

    object Radius {
        val card = 26.dp
        val poster = 16.dp
    }
}

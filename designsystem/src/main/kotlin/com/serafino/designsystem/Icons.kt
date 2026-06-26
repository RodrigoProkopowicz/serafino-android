package com.serafino.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.ui.graphics.vector.ImageVector
import com.serafino.domain.entities.BrewMethod
import com.serafino.domain.entities.Difficulty

/**
 * Mapeo de tokens semánticos (los nombres que en iOS eran SF Symbols) a `ImageVector` de
 * Material. Centraliza la resolución de íconos para que el dominio quede libre de UI.
 */

/** Ícono representativo del método de preparación. */
val BrewMethod.icon: ImageVector
    get() = when (this) {
        BrewMethod.Espresso -> Icons.Filled.Coffee
        BrewMethod.PourOverV60 -> Icons.Filled.ChangeHistory
        BrewMethod.Aeropress -> Icons.Filled.LocalCafe
        BrewMethod.FrenchPress -> Icons.Filled.LocalCafe
        BrewMethod.MokaPot -> Icons.Filled.LocalFireDepartment
        BrewMethod.ColdBrew -> Icons.Filled.AcUnit
        BrewMethod.Chemex -> Icons.Filled.HourglassEmpty
    }

/** Ícono del medidor de dificultad. */
val Difficulty.icon: ImageVector
    get() = when (this) {
        Difficulty.Easy -> Icons.Filled.Speed
        Difficulty.Medium -> Icons.Filled.Speed
        Difficulty.Hard -> Icons.Filled.Speed
    }

/** Resuelve un token de ícono (loyalty/transacciones/recompensas) a un `ImageVector`. */
fun symbolIcon(token: String): ImageVector = when (token) {
    "leaf" -> Icons.Filled.Spa
    "cup" -> Icons.Filled.LocalCafe
    "star" -> Icons.Outlined.Star
    "crown" -> Icons.Filled.EmojiEvents
    "flame" -> Icons.Filled.LocalFireDepartment
    "bag" -> Icons.Outlined.ShoppingBag
    "gift" -> Icons.Outlined.CardGiftcard
    "redeem" -> Icons.Outlined.Redeem
    "undo" -> Icons.Outlined.Undo
    "calendar_clock" -> Icons.Outlined.CalendarMonth
    else -> Icons.Outlined.CardGiftcard
}

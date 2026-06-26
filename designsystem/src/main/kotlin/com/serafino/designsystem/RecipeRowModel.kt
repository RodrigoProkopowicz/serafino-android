package com.serafino.designsystem

import com.serafino.domain.entities.BrewMethod
import com.serafino.domain.entities.Difficulty

/**
 * View model liviano de una fila de receta, compartido por Catálogo y Favoritos. Lo arman los
 * interactors con strings ya formateados (sensibles a unidades). Espeja `RecipeRowModel` de iOS.
 */
data class RecipeRowModel(
    val id: String,
    val name: String,
    val method: BrewMethod,
    val summary: String,
    val ratioText: String,
    val timeText: String,
    val difficulty: Difficulty,
    val isFavorite: Boolean,
)

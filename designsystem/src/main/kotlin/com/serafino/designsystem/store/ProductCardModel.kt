package com.serafino.designsystem.store

import com.serafino.domain.entities.store.Pricing

/**
 * View model liviano de la tarjeta de producto del catálogo. Lo arma el interactor con el precio
 * efectivo ya resuelto (formato Estándar). Espeja `ProductCardModel` de iOS.
 */
data class ProductCardModel(
    val id: String,
    val name: String,
    val origin: String,
    val roast: String,
    val imageURL: String?,
    val accent: List<String>,
    val pricing: Pricing,
    val badge: String?,
    val notes: List<String> = emptyList(),
) {
    /** Línea secundaria "Origen · Tueste". */
    val subtitle: String
        get() = listOf(origin, roast).filter { it.isNotEmpty() }.joinToString(" · ")

    /** Notas de cata resumidas (hasta 3). */
    val notesText: String
        get() = notes.take(3).joinToString(" · ")
}

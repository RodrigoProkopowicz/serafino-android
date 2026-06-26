package com.serafino.domain.entities.store

import java.util.Date

/** Reseña verificada de un producto. Espeja `ProductReview` de iOS. */
data class ProductReview(
    val rating: Int,
    val text: String,
    val createdAt: Date?,
) {
    val id: String get() = "$rating|${createdAt?.time ?: 0}|${text.hashCode()}"
}

/** Conjunto de reseñas de un producto. Espeja `ProductReviews` de iOS. */
data class ProductReviews(
    val reviews: List<ProductReview>,
    val count: Int,
) {
    /** Promedio de calificaciones (null si no hay reseñas). */
    val average: Double?
        get() = if (reviews.isEmpty()) null
        else reviews.sumOf { it.rating }.toDouble() / reviews.size

    companion object {
        val empty = ProductReviews(reviews = emptyList(), count = 0)
    }
}

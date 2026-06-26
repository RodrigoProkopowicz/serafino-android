package com.serafino.domain.entities.store

/**
 * Una línea del carrito: un café en un formato puntual, con cantidad. El precio unitario es el
 * efectivo a mostrar; el backend lo recalcula al cobrar. Espeja `CartLine` de iOS.
 */
data class CartLine(
    val productID: String,
    val name: String,
    val image: String,
    val formatLabel: String,
    val formatWeight: String,
    /** Precio unitario efectivo del formato, en ARS. */
    val unitPrice: Int,
    val quantity: Int,
) {
    /** Clave única de la línea: producto + formato (misma celda del carrito). */
    val id: String get() = "$productID|$formatWeight"

    /** Subtotal de la línea (unitario × cantidad). */
    val lineTotal: Int get() = unitPrice * quantity
}

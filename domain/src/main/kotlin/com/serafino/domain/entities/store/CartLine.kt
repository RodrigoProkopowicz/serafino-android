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
    /**
     * El formato ya trae un descuento propio (promo del catálogo o de la medida puntual) o es un
     * combo: el código promocional NO se apila encima (un producto lleva UN ÚNICO descuento). Se
     * congela al agregar, igual que `unitPrice`. Espeja `item.discounted` de `resolveOrderItems`.
     */
    val discounted: Boolean = false,
) {
    /** Clave única de la línea: producto + formato (misma celda del carrito). */
    val id: String get() = "$productID|$formatWeight"

    /** Subtotal de la línea (unitario × cantidad). */
    val lineTotal: Int get() = unitPrice * quantity

    companion object {
        /**
         * Tope de unidades por línea. Espeja `MAX_ITEM_QUANTITY` del backend
         * (`functions/lib/pricing.js`): el servidor recorta la cantidad a 1..99 al cobrar, así que
         * el cliente la acota igual para que el total mostrado nunca supere al cobrado (ARQ-1).
         */
        const val MAX_ITEM_QUANTITY = 99
    }
}

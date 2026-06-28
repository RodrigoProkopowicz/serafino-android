package com.serafino.domain.services

import com.serafino.domain.entities.store.Pricing
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Previsualización de totales del checkout (función pura, testeable). Espeja 1:1 la composición
 * del backend (`functions/lib/pricing.js`): UN ÚNICO descuento por producto. Quedan EXCLUIDOS del
 * código promocional: el producto del Café del Día (recibe solo su % dirigido, con tope ARS) y los
 * ítems que YA traen descuento propio del catálogo/medida puntual o son combos (`Line.discounted`,
 * igual que `applyPromoToOrder`). El código aplica al resto del carrito. ARQ-1: lo que se muestra
 * acá == lo que cobra el servidor.
 */
object OrderPricing {

    /** Una línea del carrito para el cálculo. */
    data class Line(
        val productID: String,
        val unitPrice: Int,
        val quantity: Int,
        /** El ítem ya trae descuento propio (catálogo/medida puntual) o es combo: excluido del código. */
        val discounted: Boolean = false,
    )

    /** Descuento dirigido del Café del Día sobre un producto puntual del carrito. */
    data class CafeDelDia(
        val productID: String,
        val pct: Int,
        val maxDiscountARS: Int?,
    )

    /** Totales a mostrar en el resumen (enteros ARS). */
    data class Totals(
        val subtotal: Int,
        val discount: Int,
        val total: Int,
    )

    /**
     * Calcula subtotal/descuento/total aplicando, por línea, UN SOLO descuento: la línea del Café
     * del Día recibe su % dirigido (con tope); un ítem que ya trae descuento propio o es combo
     * (`discounted`) conserva su precio; el resto recibe el código si hay uno válido.
     */
    fun preview(lines: List<Line>, promoPercent: Int?, cafeDelDia: CafeDelDia?): Totals {
        val subtotal = lines.sumOf { it.unitPrice * it.quantity }
        val total = lines.fold(0) { partial, line ->
            val lineTotal = line.unitPrice * line.quantity
            when {
                cafeDelDia != null && cafeDelDia.productID == line.productID ->
                    partial + cafeDelDiaLineTotal(lineTotal, line.quantity, cafeDelDia.pct, cafeDelDia.maxDiscountARS)
                // Un producto lleva UN ÚNICO descuento: el código no se apila sobre un ítem que ya
                // trae promo del catálogo/medida puntual ni sobre un combo (espeja applyPromoToOrder).
                line.discounted -> partial + lineTotal
                promoPercent != null ->
                    partial + Pricing.applyPercent(line.unitPrice, promoPercent) * line.quantity
                else -> partial + lineTotal
            }
        }
        return Totals(subtotal = subtotal, discount = max(0, subtotal - total), total = total)
    }

    /**
     * Total de la línea del Café del Día tras su descuento (espeja `applyVoucherToOrder`):
     * `round(lineTotal * pct / 100)`, acotado por `maxDiscountARS`, y el precio unitario nuevo se
     * reparte con `floor` entre las unidades.
     */
    internal fun cafeDelDiaLineTotal(lineTotal: Int, quantity: Int, pct: Int, cap: Int?): Int {
        val percent = max(0, min(100, pct))
        if (percent <= 0 || quantity <= 0) return lineTotal
        var discount = (lineTotal.toDouble() * percent.toDouble() / 100).roundToInt()
        if (cap != null && cap > 0) discount = min(discount, cap)
        if (discount <= 0) return lineTotal
        val newUnit = max(0, (lineTotal - discount) / quantity) // floor por unidad
        return newUnit * quantity
    }
}

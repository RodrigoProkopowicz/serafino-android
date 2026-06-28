package com.serafino.domain.entities.store

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Reglas de precio efectivo y promoción, portadas 1:1 desde `shared/catalog.mjs` del proyecto
 * CoffeeStore (ARQ-1: precio mostrado = precio cobrado). Precios enteros en ARS.
 * Espeja `Pricing` de iOS.
 */
data class Pricing(
    /** ¿Hay promo vigente? */
    val active: Boolean,
    /** Precio de lista. */
    val original: Int,
    /** Precio efectivo (lo que se paga). */
    val price: Int,
    /** Precio de promo informado (0 si no hay). */
    val promoPrice: Int,
    /** Descuento redondeado, en porcentaje (0 si no hay promo). */
    val discountPct: Int,
) {
    companion object {
        val zero = Pricing(active = false, original = 0, price = 0, promoPrice = 0, discountPct = 0)

        /**
         * Regla de precio efectivo (espeja `getPricing`). Promo válida = activa, con
         * `promoPrice` > 0 y menor al precio de lista.
         */
        fun resolve(price: Int, promoPrice: Int?, promoActive: Boolean): Pricing {
            val original = max(0, price)
            val promo = max(0, promoPrice ?: 0)
            val active = promoActive && promo > 0 && promo < original
            val discount = if (active && original > 0) {
                ((1 - promo.toDouble() / original.toDouble()) * 100).roundToInt()
            } else 0
            return Pricing(
                active = active,
                original = original,
                price = if (active) promo else original,
                promoPrice = promo,
                discountPct = discount,
            )
        }

        /**
         * Aplica un porcentaje de descuento (código promocional) a un precio unitario y
         * redondea a entero ARS, igual que el backend al cobrar (espeja `applyPercent`).
         */
        fun applyPercent(price: Int, percent: Int): Int =
            (price.toDouble() * (100 - percent).toDouble() / 100).roundToInt()
    }
}

/** Precio efectivo del producto (formato Estándar / nivel superior). */
val Product.pricing: Pricing
    get() = Pricing.resolve(price = price, promoPrice = promoPrice, promoActive = promoActive)

/**
 * Precio efectivo de un formato puntual (espeja `getFormatPricing`). La promo la gobierna el
 * `promoActive` del FORMATO si lo trae; si no, hereda la del producto (descuento de todo el café
 * vs. de una medida puntual). Si el formato no existe, cae al precio de nivel superior (Estándar
 * 250 g).
 */
fun Product.pricing(weight: String): Pricing {
    val format = formats.firstOrNull { it.weight == weight }
    return if (format != null) {
        Pricing.resolve(
            price = format.price,
            promoPrice = format.promoPrice,
            promoActive = format.promoActive ?: promoActive,
        )
    } else pricing
}

/** ¿El producto tiene ficha de origen con contenido? (espeja `hasFicha`). */
val Product.hasFicha: Boolean
    get() = ficha?.hasContent ?: false

/** ¿Es un combo? (espeja `isCombo`). */
val Product.isCombo: Boolean
    get() = !comboItems.isNullOrEmpty()

/**
 * Intensidad 0–10 parseada de la descripción ("Intensidad 4/10", "5,5/10" → toma el primer
 * número). `null` si no aparece.
 */
val Product.intensity: Double?
    get() {
        val regex = Regex("""Intensidad\s+(\d+(?:[.,]\d+)?)""", RegexOption.IGNORE_CASE)
        val match = regex.find(description) ?: return null
        return match.groupValues[1].replace(",", ".").toDoubleOrNull()
    }

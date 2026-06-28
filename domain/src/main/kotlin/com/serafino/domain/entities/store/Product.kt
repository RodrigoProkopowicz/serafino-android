package com.serafino.domain.entities.store

/**
 * Café del catálogo de la tienda. Espeja el documento de Firestore (colección `products`).
 * Los precios son enteros en ARS; el formato Estándar (250 g) es el de nivel superior.
 * Espeja `Product` de iOS.
 */
data class Product(
    val id: String,
    val name: String,
    /** Path relativo de la imagen (`/img/products/…jpg`). Ver `BackendConfig.imageURL`. */
    val image: String,
    val origin: String,
    val roast: String,
    /** Peso del formato Estándar (default del producto). */
    val weight: String,
    /** Precio de lista del formato Estándar, en ARS. */
    val price: Int,
    val promoActive: Boolean,
    /** Precio de preventa/promo del formato Estándar, en ARS (si hay promo). */
    val promoPrice: Int?,
    /** Dos colores HEX para el degradado de marca de la tarjeta. */
    val accent: List<String>,
    /** Notas de cata. */
    val notes: List<String>,
    /** Etiqueta opcional (ej. "Exclusivo"). */
    val badge: String?,
    /** Destacado en el hero ("Café de la semana"). */
    val featured: Boolean,
    /** Oculto: no se vende (se filtra al cargar el catálogo). */
    val hidden: Boolean,
    /** Formatos disponibles (Degustación 125 g, Estándar 250 g, Rendidor 500 g). */
    val formats: List<ProductFormat>,
    val description: String,
    /** Ficha de origen (alimenta la sección "Descubrir Origen"). */
    val ficha: Ficha?,
    /** Componentes, si el producto es un combo. */
    val comboItems: List<ComboItem>?,
)

/** Un formato de venta del café (tamaño + precio propio). */
data class ProductFormat(
    val label: String,
    val weight: String,
    val price: Int,
    val promoPrice: Int?,
    /**
     * Promo del formato: si lo trae, gobierna la promo SOLO de este tamaño; si es `null`, hereda la
     * del producto. Así el descuento puede ser de todo el café o de una medida puntual. Espeja el
     * `promoActive` por formato del backend (`shared/catalog.mjs`/`functions/lib/pricing.js`).
     */
    val promoActive: Boolean? = null,
) {
    val id: String get() = weight
}

/** Ficha de origen del café (texto plano, todo opcional). */
data class Ficha(
    val region: String?,
    val altitud: String?,
    val variedad: String?,
    val proceso: String?,
    val productor: String?,
    val cosecha: String?,
    val historia: String?,
) {
    /** ¿Tiene al menos un campo con contenido? (espeja `hasContent`). */
    val hasContent: Boolean
        get() = listOf(region, altitud, variedad, proceso, productor, cosecha, historia)
            .any { !it.isNullOrBlank() }

    /** Pares (etiqueta, valor) no vacíos, en orden de presentación. */
    val fields: List<Pair<String, String>>
        get() = listOf(
            "Región" to region,
            "Altitud" to altitud,
            "Variedad" to variedad,
            "Proceso" to proceso,
            "Productor" to productor,
            "Cosecha" to cosecha,
        ).mapNotNull { (label, value) ->
            if (value.isNullOrBlank()) null else label to value
        }
}

/** Componente de un combo (un café + formato + cantidad de envases). */
data class ComboItem(
    val productId: String,
    val name: String,
    val format: String,
    val qty: Int,
)

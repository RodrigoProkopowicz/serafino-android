package com.serafino.domain.services

import com.serafino.domain.entities.BrewMethod
import com.serafino.domain.entities.MeasurementSystem
import com.serafino.domain.entities.Recipe
import com.serafino.domain.entities.TemperatureUnit
import com.serafino.domain.entities.loyalty.DirectRedeemOutcome
import com.serafino.domain.entities.loyalty.LoyaltyAccount
import com.serafino.domain.entities.loyalty.LoyaltyConfig
import com.serafino.domain.entities.loyalty.LoyaltyVoucher
import com.serafino.domain.entities.loyalty.RedeemOutcome
import com.serafino.domain.entities.store.CartLine
import com.serafino.domain.entities.store.Product
import com.serafino.domain.entities.store.ProductReviews

/**
 * Contratos de servicios del dominio (las "fronteras" con datos/persistencia). Las
 * implementaciones viven en el módulo `data`. Espejan los protocolos de servicio de iOS.
 */

/** Fuente de recetas de la app. Hoy un catálogo estático curado. */
interface RecipeCatalogProviding {
    fun allRecipes(): List<Recipe>
    fun recipe(id: String): Recipe?
    fun recipes(method: BrewMethod): List<Recipe>
}

/** Almacén de favoritos. La implementación persiste y publica `FavoriteToggledEvent`. */
interface FavoritesStoring {
    val favoriteIDs: Set<String>
    fun isFavorite(id: String): Boolean
    fun toggle(id: String)
    fun setFavorite(id: String, value: Boolean)
}

/** Preferencias del usuario. La implementación persiste y publica `SettingsChangedEvent`. */
interface SettingsStoring {
    var measurementSystem: MeasurementSystem
    var temperatureUnit: TemperatureUnit
    var hapticsEnabled: Boolean
}

/** Persistencia del perfil de contacto + envío del comprador (un único `CheckoutForm`). */
interface CheckoutProfileStoring {
    /** Datos guardados del usuario, o `null` si nunca completó un pedido. */
    fun load(): CheckoutForm?
    /** Guarda/actualiza los datos para prellenarlos la próxima vez. */
    fun save(form: CheckoutForm)
}

/** Carrito local. La implementación persiste y publica `CartChangedEvent`. */
interface CartStoring {
    /** Líneas actuales, en orden de agregado. */
    val lines: List<CartLine>
    /** Cantidad total de unidades (para el badge de la bolsa). */
    val count: Int
    /** Subtotal a mostrar, en ARS. */
    val subtotal: Int

    /** Agrega una línea; si ya existe (mismo producto + formato), suma la cantidad. */
    fun add(line: CartLine)
    /** Fija la cantidad de una línea; `<= 0` la elimina. */
    fun setQuantity(id: String, quantity: Int)
    /** Elimina una línea. */
    fun remove(id: String)
    /** Vacía el carrito (p. ej. tras un pago aprobado). */
    fun clear()
}

/** Fuente del catálogo de la tienda. La implementación lee Firestore y cachea. */
interface ProductCatalogProviding {
    /** Carga el catálogo visible (filtra ocultos) y actualiza la caché interna. */
    suspend fun loadProducts(): List<Product>
    /** Producto por id desde la caché (null si el catálogo aún no se cargó). */
    fun product(id: String): Product?
}

/** Reseñas verificadas de un producto (solo lectura). */
interface ReviewsProviding {
    suspend fun reviews(productID: String): ProductReviews
}

/**
 * Fuente de la cuenta de fidelidad. Frontera con el backend; hoy la cumple un provider de muestra
 * y mañana una implementación de red. Espeja `LoyaltyProviding` de iOS.
 */
interface LoyaltyProviding {
    /** Config PÚBLICA del programa (no requiere sesión). */
    suspend fun loadPublicConfig(): LoyaltyConfig

    /** Estado de fidelidad del usuario (incluye canje y Café del Día con estado por usuario). */
    suspend fun loadAccount(): LoyaltyAccount

    /** Canjea un premio por granos. Devuelve el vale emitido + el saldo nuevo. */
    suspend fun redeem(rewardId: String, idempotencyKey: String?): RedeemOutcome

    /** Canje DIRECTO de un vale (café gratis / merch sin compra, entregado por envío). */
    suspend fun redeemDirect(
        voucherId: String,
        productId: String?,
        grind: String?,
        contact: CheckoutForm,
    ): DirectRedeemOutcome

    /** Vales del usuario (sus canjes, para usar en el checkout o pedir directo con envío). */
    suspend fun vouchers(): List<LoyaltyVoucher>
}

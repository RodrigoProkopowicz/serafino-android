package com.serafino.architecture

/**
 * Contrato de analítica de la app, en Architecture para que los features lo usen por inversión
 * de dependencias SIN conocer Firebase. La implementación real vive en el app target.
 * Espeja `AnalyticsTracking` de iOS.
 */

/** Evento de analítica de alto nivel (vocabulario de dominio, no de la plataforma). */
sealed interface AnalyticsEvent {
    /** Vista de pantalla (p. ej. "store_catalog", "recipes"). */
    data class Screen(val name: String) : AnalyticsEvent
    /** El usuario vio el detalle de un ítem (café o receta). */
    data class ViewItem(val id: String, val name: String, val value: Int?) : AnalyticsEvent
    /** Se agregó un producto al carrito. */
    data class AddToCart(val id: String, val name: String, val value: Int, val quantity: Int) : AnalyticsEvent
    /** Empezó el checkout. */
    data class BeginCheckout(val value: Int, val itemCount: Int) : AnalyticsEvent
    /** Compra confirmada (pago aprobado). */
    data class Purchase(val orderID: String, val value: Int) : AnalyticsEvent
    /** Terminó de preparar una receta en el timer. */
    data class BrewCompleted(val recipeID: String) : AnalyticsEvent
}

/** Sumidero de eventos de analítica. La app inyecta Firebase; por defecto [NoOpAnalytics]. */
interface AnalyticsTracking {
    fun track(event: AnalyticsEvent)
}

/** Implementación nula por defecto: permite construir interactores sin analítica (tests). */
class NoOpAnalytics : AnalyticsTracking {
    override fun track(event: AnalyticsEvent) {}
}

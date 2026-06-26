package com.serafino.architecture

/**
 * Eventos cross-module entregados por el [EventBus] compartido. Espejan `AppEvents` de iOS.
 */

/** Una receta cambió su estado de favorito. Sincroniza catálogo, detalle y favoritos. */
data class FavoriteToggledEvent(val recipeID: String, val isFavorite: Boolean) : BusEvent

/** Cambiaron las preferencias de unidades/temperatura. Los interactores re-formatean. */
class SettingsChangedEvent : BusEvent

/** El usuario terminó de preparar una receta en el timer. */
data class BrewCompletedEvent(val recipeID: String) : BusEvent

/** Cambió el contenido del carrito (alta/baja/cantidad/vaciado). Mantiene el badge en sync. */
data class CartChangedEvent(val count: Int) : BusEvent

/** El usuario ganó granos de fidelidad (p. ej. tras una compra aprobada). El Perfil refresca. */
data class BeansEarnedEvent(val amount: Int) : BusEvent

/** Cambió la sesión del usuario (login/logout/restauración). El Perfil alterna gate/contenido. */
data class AuthChangedEvent(val isSignedIn: Boolean) : BusEvent

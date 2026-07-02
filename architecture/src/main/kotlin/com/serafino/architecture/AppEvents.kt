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

/**
 * Se emite cuando una compra queda APROBADA (lo publica OrderStatus al confirmar el pago).
 * El Perfil lo escucha para reconciliar los granos contra el webhook de Mercado Pago
 * (reintentos escalonados hasta ver el saldo acreditado).
 */
data class PurchaseCompletedEvent(val orderID: String) : BusEvent

/**
 * Se emite cuando un canje se concreta (vale emitido por granos o vale consumido con envío).
 * El Perfil lo escucha para refrescar la cuenta y el Checkout para recargar sus vales.
 */
class RewardRedeemedEvent : BusEvent

/**
 * Se emite UNA sola vez por retorno al foreground (lo publica la raíz de la app vía el
 * lifecycle de la Activity). Los interactores que cachean datos remotos lo escuchan y
 * refrescan solo si su TTL venció — volver de un background corto no cuesta red.
 */
class AppDidBecomeActiveEvent : BusEvent

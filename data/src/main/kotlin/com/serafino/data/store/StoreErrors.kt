package com.serafino.data.store

/** Errores de la API del backend CoffeeStore. Espejan `APIError`/`CatalogError` de iOS. */
sealed class ApiException(message: String) : Exception(message) {
    class Server(val serverMessage: String) : ApiException(serverMessage)
    data object InvalidResponse : ApiException("Respuesta inválida del servidor.")
    data object CatalogUnavailable :
        ApiException("No pudimos cargar el catálogo. Revisá tu conexión y probá de nuevo.")
}

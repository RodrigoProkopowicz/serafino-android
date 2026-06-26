package com.serafino.domain

import java.net.URI

/**
 * Endpoints del backend de la tienda (proyecto CoffeeStore, marca Serafino) y FRONTERA DE
 * CONFIANZA del cliente: toda URL que nace de datos del servidor (init_point de pago, imágenes)
 * o de un id que viaja en el path se valida acá, fail-closed. Espeja `BackendConfig` de iOS.
 */
object BackendConfig {
    /** Sitio de la tienda. Las rutas de la API se reescriben a la Cloud Function `api`. */
    const val webBase = "https://coffeestore-5322d.web.app"

    /** Proyecto Firebase y API key WEB (pública). */
    const val firestoreProjectID = "coffeestore-5322d"
    const val firebaseWebAPIKey = "AIzaSyDAWvesfP4d7mb7LklxGspnm3D29qeXeD0"

    /** Lectura pública del catálogo vía `runQuery` (solo productos con `hidden == false`). */
    val productsQueryURL: String
        get() = "https://firestore.googleapis.com/v1/projects/$firestoreProjectID" +
            "/databases/(default)/documents:runQuery" +
            "?key=$firebaseWebAPIKey"

    /** Cuerpo de la consulta del catálogo: productos con `hidden == false` (hasta 300). */
    val productsQueryBody: String
        get() = """{"structuredQuery":{"from":[{"collectionId":"products"}],"where":{"fieldFilter":{"field":{"fieldPath":"hidden"},"op":"EQUAL","value":{"booleanValue":false}}},"limit":300}}"""

    /** Construye una URL de la API a partir de un path relativo (sin barra inicial). */
    fun apiURL(path: String): String = "$webBase/api/$path"

    // MARK: - Frontera de confianza

    /** Dominios de Mercado Pago donde puede vivir un `init_point` de Checkout Pro. */
    private val mercadoPagoHosts = setOf(
        "mercadopago.com", "mercadopago.com.ar", "mercadopago.com.br",
        "mercadopago.com.mx", "mercadopago.com.co", "mercadopago.com.uy",
        "mercadopago.com.pe", "mercadopago.com.ec", "mercadopago.cl",
        "mercadolibre.com.ar", "mpago.la",
    )

    /**
     * Valida y devuelve la URL de pago a partir de la respuesta del backend. Exige `https` y un
     * host de Mercado Pago (dominio exacto o subdominio); cualquier otra cosa devuelve `null`.
     */
    fun paymentURL(raw: String): String? {
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "https") return null
        val host = uri.host?.lowercase() ?: return null
        val ok = mercadoPagoHosts.any { host == it || host.endsWith(".$it") }
        return if (ok) raw else null
    }

    /**
     * Sanea un id para usarlo como ÚNICO segmento de path. Rechaza (`null`) ids vacíos, con
     * separadores (`/`, `\`), con path traversal (`..`) o caracteres de control. El resto se
     * percent-codifica. Fail-closed: el llamador corta si es `null`.
     */
    fun safePathSegment(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        if (value.contains("/")) return null
        if (value.contains("\\")) return null
        if (value.contains("..")) return null
        if (value.any { it.isISOControl() }) return null
        return percentEncodePathSegment(value)
    }

    /**
     * Resuelve la URL de una imagen del catálogo. `https` se respeta; `http` se promueve a
     * `https`; otros esquemas absolutos se rechazan; un path relativo se ancla al host de la
     * tienda. Espeja `imageURL` de iOS.
     */
    fun imageURL(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val lower = trimmed.lowercase()
        if (lower.startsWith("https://")) return trimmed
        if (lower.startsWith("http://")) return "https://" + trimmed.substring("http://".length)
        // Esquema absoluto que no es http(s) (`data:`, `file:`, `javascript:`…) → rechazo.
        if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(trimmed)) return null
        // Path relativo: anclado al host de la tienda. Se descartan TODAS las barras iniciales.
        val path = trimmed.dropWhile { it == '/' }
        if (path.isEmpty()) return null
        return "$webBase/$path"
    }

    // Conjunto permitido para un segmento de path (RFC 3986 pchar sin "/"): unreserved + sub-delims + ":@".
    private const val pathSegmentAllowed =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~!\$&'()*+,;=:@"

    private fun percentEncodePathSegment(value: String): String {
        val out = StringBuilder()
        for (byte in value.toByteArray(Charsets.UTF_8)) {
            val c = byte.toInt().toChar()
            if (byte >= 0 && pathSegmentAllowed.indexOf(c) >= 0) {
                out.append(c)
            } else {
                out.append('%')
                out.append("%02X".format(byte.toInt() and 0xFF))
            }
        }
        return out.toString()
    }
}

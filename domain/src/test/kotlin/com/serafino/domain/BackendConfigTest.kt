package com.serafino.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Paridad con `BackendConfigTests` de iOS (frontera de confianza, adversarial). */
class BackendConfigTest {

    private val storeHost = "https://coffeestore-5322d.web.app/"

    @Test fun paymentURLAcceptsMercadoPago() {
        val ok = listOf(
            "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=x",
            "https://sandbox.mercadopago.com.ar/checkout",
            "https://mercadopago.com.ar/x",
            "https://mercadopago.com/x",
            "https://mpago.la/abc",
        )
        for (p in ok) assertEquals("debía aceptar $p", p, BackendConfig.paymentURL(p))
    }

    @Test fun paymentURLRejectsUnsafe() {
        val bad = listOf(
            "http://www.mercadopago.com.ar/x",
            "https://evil.example/x",
            "https://mercadopago.com.ar.evil.com/x",
            "https://notmercadopago.com.ar/x",
            "https://mercadopago.com.evil/x",
            "javascript:alert(1)",
            "data:text/html,<script>",
            "",
            "   ",
        )
        for (p in bad) assertNull("debía rechazar '$p'", BackendConfig.paymentURL(p))
    }

    @Test fun safePathSegmentAcceptsAndEncodes() {
        assertEquals("order-7", BackendConfig.safePathSegment("order-7"))
        assertEquals("ABC_123.def", BackendConfig.safePathSegment("ABC_123.def"))
        assertEquals("ab%20cd", BackendConfig.safePathSegment("ab cd"))
        assertEquals("caf%C3%A9", BackendConfig.safePathSegment("café"))
        assertEquals("order-7", BackendConfig.safePathSegment("  order-7  "))
    }

    @Test fun safePathSegmentRejectsUnsafe() {
        for (bad in listOf("", "   ", "a/b", "..", "../admin", "x..y", "a\\b", "a\nb", "\t")) {
            assertNull("debía rechazar '$bad'", BackendConfig.safePathSegment(bad))
        }
    }

    @Test fun safePathSegmentNeverLeaksSlash() {
        val segment = BackendConfig.safePathSegment("a:b@c;d=e")
        assertFalse(segment!!.contains("/"))
    }

    @Test fun imageURLEnforcesTLS() {
        assertEquals("https://cdn.x/a.jpg", BackendConfig.imageURL("https://cdn.x/a.jpg"))
        assertEquals("https://cdn.x/a.jpg", BackendConfig.imageURL("http://cdn.x/a.jpg"))
        assertEquals("https://cdn.x/a.jpg", BackendConfig.imageURL("HTTP://cdn.x/a.jpg"))
    }

    @Test fun imageURLAnchorsRelativeToStoreHost() {
        assertTrue(BackendConfig.imageURL("/img/a.jpg")!!.startsWith(storeHost))
        assertTrue(BackendConfig.imageURL("img/a.jpg")!!.startsWith(storeHost))
        // Protocolo-relativo: no salta de autoridad, queda anclado al host de la tienda.
        assertTrue(BackendConfig.imageURL("//evil.com/x.jpg")!!.startsWith(storeHost))
    }

    @Test fun imageURLRejectsDangerousSchemes() {
        for (bad in listOf("data:image/png;base64,AAAA", "file:///etc/passwd", "javascript:alert(1)", "", "   ", "/")) {
            assertNull("debía rechazar '$bad'", BackendConfig.imageURL(bad))
        }
    }
}

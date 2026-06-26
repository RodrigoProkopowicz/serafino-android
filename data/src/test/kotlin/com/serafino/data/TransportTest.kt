package com.serafino.data

import com.serafino.architecture.AuthProviding
import com.serafino.architecture.AuthUser
import com.serafino.data.loyalty.BackendLoyaltyProvider
import com.serafino.data.store.ApiException
import com.serafino.data.store.CoffeeStoreAPIClient
import com.serafino.data.store.FirestoreProductCatalog
import com.serafino.domain.services.CheckoutForm
import com.serafino.domain.services.CheckoutRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Tests de TRANSPORTE de los clientes de red (status codes, errores, headers, paths, caché). El
 * parsing se cubre en los tests de mappers; acá la red se stubbea con un interceptor OkHttp que
 * devuelve respuestas canned (el análogo de `MockURLProtocol` de iOS). Espeja `TransportTests`.
 */
private class FakeTransport : Interceptor {
    var handler: (Request) -> Pair<Int, String> = { 200 to "{}" }
    var failWith: IOException? = null
    var lastRequest: Request? = null
    var lastBody: String? = null

    fun respond(status: Int, body: String) { failWith = null; handler = { status to body } }
    fun byPath(map: (String) -> Pair<Int, String>) { failWith = null; handler = { map(it.url.encodedPath) } }
    fun failTransport() { failWith = IOException("not connected") }

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        lastRequest = req
        lastBody = req.body?.let { val b = Buffer(); it.writeTo(b); b.readUtf8() }
        failWith?.let { throw it }
        val (code, body) = handler(req)
        return Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(code).message("stub")
            .body(body.toResponseBody("application/json".toMediaType())).build()
    }
}

private fun clientWith(t: FakeTransport): OkHttpClient = OkHttpClient.Builder().addInterceptor(t).build()

private class FakeAuth(private val token: String? = "tok-abc") : AuthProviding {
    override val currentUser: AuthUser? = null
    override suspend fun signIn() {}
    override fun signOut() {}
    override suspend fun idToken(): String? = token
}

/** Transporte del cliente de la API CoffeeStore. */
class CoffeeStoreApiClientTransportTest {
    private val t = FakeTransport()
    private fun client(token: String? = "tok-abc") = CoffeeStoreAPIClient(FakeAuth(token), clientWith(t))

    private val sampleRequest = CheckoutRequest(
        items = listOf(CheckoutRequest.Item("aurora", "250 g", 1)),
        payer = CheckoutRequest.Payer("Ana", "ana@mail.com", "1122"),
        shipping = CheckoutRequest.Shipping("Calle 1", "CABA", "BA", "1000"),
        promoCode = null, grind = "whole",
    )

    @Test fun fetchOrderOk() = runBlocking {
        t.respond(200, """{"id":"order-7","status":"approved","total":17000,"currency":"ARS"}""")
        val order = client().fetchOrder("order-7")
        assertEquals("order-7", order.id)
        assertEquals(17000, order.total)
        assertEquals("GET", t.lastRequest?.method)
        assertTrue(t.lastRequest!!.url.toString().contains("/api/order/order-7"))
    }

    @Test fun fetchOrderServerError() = runBlocking {
        t.respond(500, """{"error":"Pedido inexistente"}""")
        val ex = runCatching { client().fetchOrder("x") }.exceptionOrNull()
        assertTrue(ex is ApiException.Server)
        assertEquals("Pedido inexistente", (ex as ApiException.Server).serverMessage)
    }

    @Test fun fetchOrderEncodesId() = runBlocking {
        t.respond(200, """{"status":"pending"}""")
        client().fetchOrder("ab cd")
        assertTrue(t.lastRequest!!.url.toString().contains("ab%20cd"))
    }

    @Test fun fetchOrderRejectsUnsafeId() = runBlocking {
        t.respond(200, """{"status":"approved"}""")
        for (bad in listOf("../admin", "a/b", "..", "", "a\\b")) {
            val ex = runCatching { client().fetchOrder(bad) }.exceptionOrNull()
            assertTrue("debía rechazar $bad", ex is ApiException)
        }
        assertNull(t.lastRequest) // ningún request salió a la red
    }

    @Test fun createPreferenceOk() = runBlocking {
        t.respond(200, """{"orderId":"o1","init_point":"https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=o1"}""")
        val pref = client().createPreference(sampleRequest)
        assertEquals("o1", pref.orderId)
        assertEquals("POST", t.lastRequest?.method)
        assertEquals("application/json", t.lastRequest?.body?.contentType().toString().substringBefore(";"))
        assertTrue(t.lastRequest!!.url.toString().contains("/api/create-preference"))
    }

    @Test fun createPreferenceServerError() = runBlocking {
        t.respond(422, """{"error":"Sin stock"}""")
        val ex = runCatching { client().createPreference(sampleRequest) }.exceptionOrNull()
        assertEquals("Sin stock", (ex as ApiException.Server).serverMessage)
    }

    @Test fun createPreferenceInvalidBody() = runBlocking {
        t.respond(200, """{"orderId":"o1"}""")
        assertTrue(runCatching { client().createPreference(sampleRequest) }.exceptionOrNull() is ApiException)
    }

    @Test fun validatePromoOk() = runBlocking {
        t.respond(200, """{"valid":true,"code":"CAFE10","percent":10}""")
        val promo = client().validatePromo("CAFE10")
        assertTrue(promo.valid)
        assertEquals(10, promo.percent)
    }

    @Test fun validatePromoTransportFallback() = runBlocking {
        t.failTransport()
        val promo = client().validatePromo("X")
        assertFalse(promo.valid)
        assertEquals("No se pudo validar el código.", promo.error)
    }

    @Test fun checkoutNoticeOk() = runBlocking {
        t.respond(200, """{"enabled":true,"text":"Envío gratis"}""")
        val notice = client().checkoutNotice()
        assertTrue(notice?.enabled == true)
        assertEquals("Envío gratis", notice?.text)
    }

    @Test fun checkoutNoticeFailures() = runBlocking {
        t.respond(503, "{}")
        assertNull(client().checkoutNotice())
        t.failTransport()
        assertNull(client().checkoutNotice())
    }

    @Test fun reviewsOk() = runBlocking {
        t.respond(200, """{"count":1,"reviews":[{"rating":5,"text":"Top"}]}""")
        val result = client().reviews("aurora")
        assertEquals(1, result.count)
        assertEquals(5, result.reviews.first().rating)
        assertTrue(t.lastRequest!!.url.toString().contains("/api/reviews/aurora"))
    }

    @Test fun reviewsServerError() = runBlocking {
        t.respond(500, """{"error":"boom"}""")
        assertTrue(runCatching { client().reviews("x") }.exceptionOrNull() is ApiException)
    }
}

/** Transporte del gateway del catálogo. */
class FirestoreProductCatalogTransportTest {
    private val t = FakeTransport()
    private fun catalog() = FirestoreProductCatalog(clientWith(t))

    private val twoProducts = """
    [
      {"document":{"name":"p/aurora","fields":{"name":{"stringValue":"Aurora"},"price":{"integerValue":"20000"}}}},
      {"document":{"name":"p/oculto","fields":{"name":{"stringValue":"Oculto"},"hidden":{"booleanValue":true}}}}
    ]
    """.trimIndent()

    @Test fun loadsAndCaches() = runBlocking {
        t.respond(200, twoProducts)
        val c = catalog()
        val products = c.loadProducts()
        assertEquals(listOf("aurora"), products.map { it.id }) // oculto filtrado
        assertEquals("Aurora", c.product("aurora")?.name)
        assertNull(c.product("oculto"))
        assertEquals("POST", t.lastRequest?.method)
        assertTrue(t.lastRequest!!.url.toString().contains(":runQuery"))
    }

    @Test fun productNullBeforeLoad() {
        assertNull(catalog().product("aurora"))
    }

    @Test fun nonSuccessThrowsCatalogUnavailable() = runBlocking {
        t.respond(500, "{}")
        assertTrue(runCatching { catalog().loadProducts() }.exceptionOrNull() is ApiException.CatalogUnavailable)
    }

    @Test fun malformedJsonThrows() = runBlocking {
        t.respond(200, "no-soy-json")
        assertTrue(runCatching { catalog().loadProducts() }.exceptionOrNull() != null)
    }

    @Test fun transportErrorThrows() = runBlocking {
        t.failTransport()
        assertTrue(runCatching { catalog().loadProducts() }.exceptionOrNull() != null)
    }
}

/** Transporte del provider de fidelidad backend. */
class BackendLoyaltyProviderTransportTest {
    private val t = FakeTransport()
    private fun provider(token: String? = "tok-abc") = BackendLoyaltyProvider(FakeAuth(token), client = clientWith(t))

    @Test fun publicConfigHasNoAuth() = runBlocking {
        t.respond(200, """{"pointsName":"granos","amountPerPoint":1000,"tiers":[{"key":"base","label":"Base","min":0,"cafeDelDiaPct":5}]}""")
        val config = provider(token = null).loadPublicConfig()
        assertEquals("granos", config.pointsName)
        assertEquals(5, config.tiers.first().cafeDelDiaPct)
        assertNull(t.lastRequest?.header("Authorization"))
        assertTrue(t.lastRequest!!.url.encodedPath.endsWith("/loyalty/config"))
    }

    @Test fun loadAccountDegradesGracefully() = runBlocking {
        t.byPath { path ->
            if (path.endsWith("/loyalty/me")) 200 to """{"points":450,"lifetimePoints":450,"tier":"intermedio","tierLabel":"Intermedio"}"""
            else 500 to """{"error":"caído"}"""
        }
        val account = provider().loadAccount()
        assertEquals(450, account.points)
        assertEquals(listOf("base", "intermedio", "top"), account.tiers.map { it.id })
        assertTrue(account.history.isEmpty())
    }

    @Test fun loadAccountThrowsWhenMeFails() = runBlocking {
        t.byPath { path -> if (path.endsWith("/loyalty/me")) 500 to """{"error":"x"}""" else 200 to "{}" }
        assertTrue(runCatching { provider().loadAccount() }.exceptionOrNull() is ApiException)
    }

    @Test fun redeemSendsIdempotencyKey() = runBlocking {
        t.respond(200, """{"ok":true,"voucher":{"id":"v9","type":"free_product","status":"active"},"balance":200}""")
        val outcome = provider().redeem("free-125", "idem-123")
        assertEquals("v9", outcome.voucher.id)
        assertEquals(200, outcome.balance)
        val body = JSONObject(t.lastBody!!)
        assertEquals("free-125", body.getString("rewardId"))
        assertEquals("idem-123", body.getString("idempotencyKey"))
        assertEquals("POST", t.lastRequest?.method)
        assertTrue(t.lastRequest!!.url.encodedPath.endsWith("/loyalty/redeem"))
    }

    @Test fun redeemDirectShips() = runBlocking {
        t.respond(200, """{"ok":true,"orderId":"loyalty-order-3"}""")
        val contact = CheckoutForm(name = "Ana Pérez", email = "ana@mail.com", street = "Av. Mitre 123", city = "San Miguel", province = "Buenos Aires", zip = "1663")
        val outcome = provider().redeemDirect("v1", "aurora", "media", contact)
        assertEquals("loyalty-order-3", outcome.orderId)
        assertTrue(t.lastRequest!!.url.encodedPath.endsWith("/loyalty/redeem/ship"))
        val body = JSONObject(t.lastBody!!)
        assertEquals("v1", body.getString("voucherId"))
        assertEquals("aurora", body.getString("productId"))
        assertEquals("media", body.getString("grind"))
        val payer = body.getJSONObject("payer")
        assertEquals("Ana Pérez", payer.getString("name"))
        assertEquals("ana@mail.com", payer.getString("email"))
        assertFalse(payer.has("phone")) // opcional vacío omitido
        val shipping = body.getJSONObject("shipping")
        assertEquals("Av. Mitre 123", shipping.getString("street"))
        assertEquals("1663", shipping.getString("zip"))
    }

    @Test fun redeemDirectMerchOmitsCoffeeFields() = runBlocking {
        t.respond(200, """{"ok":true,"orderId":"loyalty-order-4"}""")
        val contact = CheckoutForm(name = "Ana Pérez", email = "ana@mail.com", phone = "1122334455", street = "Av. Mitre 123", city = "San Miguel", province = "Buenos Aires", zip = "1663")
        provider().redeemDirect("v-merch", null, null, contact)
        val body = JSONObject(t.lastBody!!)
        assertEquals("v-merch", body.getString("voucherId"))
        assertFalse(body.has("productId"))
        assertFalse(body.has("grind"))
    }

    @Test fun rateLimitMessage() = runBlocking {
        t.respond(429, "{}")
        val ex = runCatching { provider().redeem("free-125", null) }.exceptionOrNull()
        assertTrue((ex as ApiException.Server).serverMessage.contains("Demasiadas solicitudes"))
    }
}

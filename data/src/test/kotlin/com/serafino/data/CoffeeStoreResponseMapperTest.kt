package com.serafino.data

import com.serafino.data.store.ApiException
import com.serafino.data.store.CoffeeStoreResponseMapper
import com.serafino.domain.entities.store.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Espeja `CoffeeStoreResponseMapperTests` de iOS (parsing de respuestas de la API → dominio). */
class CoffeeStoreResponseMapperTest {

    // MARK: Order

    @Test fun ordersApprovedWithNumericAndStringAmounts() {
        val order = CoffeeStoreResponseMapper.order(
            """
            {"id":"order-9","status":"approved","total":34000,"currency":"ARS","items":[
              {"title":"Aurora 250 g","quantity":1,"unit_price":17000},
              {"title":"Plano 250 g","quantity":"2","unit_price":"18000"}
            ]}
            """.trimIndent(),
            fallbackID = "fallback",
        )
        assertEquals("order-9", order.id)
        assertEquals(OrderStatus.Approved, order.status)
        assertEquals(34000, order.total)
        assertEquals("ARS", order.currency)
        assertEquals(2, order.items.size)
        assertEquals(17000, order.items[0].unitPrice)
        assertEquals(2, order.items[1].quantity)
        assertEquals(18000, order.items[1].unitPrice)
    }

    @Test fun ordersFallbackIdAndCurrency() {
        val order = CoffeeStoreResponseMapper.order("""{"status":"pending","total":"5000"}""", fallbackID = "the-fallback")
        assertEquals("the-fallback", order.id)
        assertEquals(OrderStatus.Pending, order.status)
        assertEquals(5000, order.total)
        assertEquals("ARS", order.currency)
        assertTrue(order.items.isEmpty())
    }

    @Test fun orderStatuses() {
        fun status(raw: String) = CoffeeStoreResponseMapper.order("""{"status":"$raw"}""", "x").status
        assertEquals(OrderStatus.InProcess, status("in_process"))
        assertEquals(OrderStatus.Rejected, status("rejected"))
        assertEquals(OrderStatus.Rejected, status("charged_back"))
        assertEquals(OrderStatus.Unknown("weird"), status("weird"))
    }

    // MARK: Reviews

    @Test fun reviewsMappedWithAverage() {
        val result = CoffeeStoreResponseMapper.reviews(
            """
            {"count":2,"reviews":[
              {"rating":5,"text":"Excelente","createdAt":1700000000000},
              {"rating":4,"text":"Muy bueno","createdAt":"1700000100000"}
            ]}
            """.trimIndent(),
        )
        assertEquals(2, result.count)
        assertEquals(5, result.reviews[0].rating)
        assertEquals("Excelente", result.reviews[0].text)
        assertEquals(1700000000000L, result.reviews[0].createdAt?.time)
        assertEquals(1700000100000L, result.reviews[1].createdAt?.time)
        assertEquals(4.5, result.average!!, 0.0001)
    }

    @Test fun reviewsCountFallback() {
        val result = CoffeeStoreResponseMapper.reviews("""{"reviews":[{"rating":3,"text":"Ok"}]}""")
        assertEquals(1, result.count)
        assertNull(result.reviews[0].createdAt)
    }

    @Test fun reviewsEmpty() {
        val result = CoffeeStoreResponseMapper.reviews("{}")
        assertTrue(result.reviews.isEmpty())
        assertEquals(0, result.count)
        assertNull(result.average)
    }

    // MARK: Promo

    @Test fun promoValid() {
        val promo = CoffeeStoreResponseMapper.promo("""{"valid":true,"code":"CAFE10","percent":10}""")
        assertTrue(promo.valid)
        assertEquals("CAFE10", promo.code)
        assertEquals(10, promo.percent)
        assertNull(promo.error)
    }

    @Test fun promoInvalid() {
        val promo = CoffeeStoreResponseMapper.promo("""{"valid":false,"error":"Código vencido"}""")
        assertFalse(promo.valid)
        assertNull(promo.percent)
        assertEquals("Código vencido", promo.error)
    }

    // MARK: Notice

    @Test fun noticeMapped() {
        val on = CoffeeStoreResponseMapper.notice("""{"enabled":true,"text":"Envío gratis esta semana"}""")
        assertTrue(on.enabled)
        assertEquals("Envío gratis esta semana", on.text)
        val off = CoffeeStoreResponseMapper.notice("{}")
        assertFalse(off.enabled)
        assertTrue(off.text.isEmpty())
    }

    // MARK: Preference

    @Test fun preferenceValid() {
        val pref = CoffeeStoreResponseMapper.preference(
            """
            {"orderId":"order-1",
             "init_point":"https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=order-1",
             "sandbox_init_point":"https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=order-1"}
            """.trimIndent(),
        )
        assertEquals("order-1", pref.orderId)
        assertEquals("https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=order-1", pref.initPoint)
        assertEquals("https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=order-1", pref.sandboxInitPoint)
    }

    @Test fun preferenceMissingInitPointThrows() {
        assertTrue(threwApi { CoffeeStoreResponseMapper.preference("""{"orderId":"order-1"}""") })
    }

    @Test fun preferenceRejectsUnsafeInitPoint() {
        val unsafe = listOf(
            "http://www.mercadopago.com.ar/checkout",
            "https://evil.example/checkout",
            "https://mercadopago.com.ar.evil.com/x",
            "https://notmercadopago.com.ar/x",
            "javascript:alert(1)",
            "the quick brown fox",
        )
        for (point in unsafe) {
            assertTrue("debía rechazar $point", threwApi {
                CoffeeStoreResponseMapper.preference("""{"orderId":"order-1","init_point":"$point"}""")
            })
        }
    }

    @Test fun preferenceDropsUnsafeSandbox() {
        val pref = CoffeeStoreResponseMapper.preference(
            """{"orderId":"order-1","init_point":"https://www.mercadopago.com.ar/checkout","sandbox_init_point":"https://evil.example/x"}""",
        )
        assertEquals("https://www.mercadopago.com.ar/checkout", pref.initPoint)
        assertNull(pref.sandboxInitPoint)
    }

    // MARK: Error message

    @Test fun errorMessage() {
        assertEquals("Sin stock", CoffeeStoreResponseMapper.errorMessage("""{"error":"Sin stock"}"""))
        assertTrue(CoffeeStoreResponseMapper.errorMessage("{}").contains("No se pudo completar"))
    }

    private fun threwApi(block: () -> Unit): Boolean =
        try { block(); false } catch (e: ApiException) { true }
}

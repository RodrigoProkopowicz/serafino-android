package com.serafino.feature.store

import com.serafino.architecture.EventBus
import com.serafino.architecture.RewardRedeemedEvent
import com.serafino.domain.entities.loyalty.LoyaltyRewardType
import com.serafino.domain.entities.store.CartLine
import com.serafino.domain.services.CheckoutField
import com.serafino.domain.services.CheckoutForm
import com.serafino.domain.services.PromoResult
import com.serafino.feature.store.checkout.CheckoutInteractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CheckoutInteractorTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun make(cartLines: List<CartLine> = listOf(cartLine(SampleProducts.aurora))): Triple<CheckoutInteractor, MockCartStore, MockCheckoutService> {
        val bus = EventBus()
        val cart = MockCartStore(bus, cartLines)
        val checkout = MockCheckoutService()
        return Triple(CheckoutInteractor(cart, checkout), cart, checkout)
    }

    private fun fillValid(i: CheckoutInteractor) {
        i.handle(CheckoutInteractor.Input.UpdateField(CheckoutField.Name, "Ana Pérez"))
        i.handle(CheckoutInteractor.Input.UpdateField(CheckoutField.Email, "ana@mail.com"))
        i.handle(CheckoutInteractor.Input.UpdateField(CheckoutField.Phone, "1122334455"))
        i.handle(CheckoutInteractor.Input.UpdateField(CheckoutField.Street, "Calle 123"))
        i.handle(CheckoutInteractor.Input.UpdateField(CheckoutField.City, "CABA"))
        i.handle(CheckoutInteractor.Input.UpdateField(CheckoutField.Province, "Buenos Aires"))
        i.handle(CheckoutInteractor.Input.UpdateField(CheckoutField.Zip, "1000"))
    }

    @Test fun validationBlocksPay() {
        val (i, _, checkout) = make()
        i.handle(CheckoutInteractor.Input.Pay)
        assertTrue(i.data.fieldErrors.isNotEmpty())
        assertTrue(checkout.createdRequests.isEmpty())
    }

    @Test fun payBuildsRequestAndPreference() {
        val (i, _, checkout) = make(listOf(cartLine(SampleProducts.aurora, 2)))
        fillValid(i)
        i.handle(CheckoutInteractor.Input.Pay)
        assertEquals(1, checkout.createdRequests.size)
        val req = checkout.createdRequests.first()
        assertEquals("aurora", req.items.first().id)
        assertEquals("250 g", req.items.first().format)
        assertEquals(2, req.items.first().quantity)
        assertEquals("ana@mail.com", req.payer.email)
        assertEquals("grano", req.grind)
        assertNull(req.promoCode)   // sin código validado no viaja nada
        assertNotNull(i.data.payment)
        assertEquals("order-1", i.data.lastOrderId)
    }

    /** Previsualizar un promo válido recalcula el total (producto sin promo de catálogo). */
    @Test fun promoPreviewRecomputesTotals() {
        val (i, _, checkout) = make(listOf(cartLine(SampleProducts.plain)))
        checkout.promoResult = PromoResult(valid = true, code = "OFF10", percent = 10, error = null)
        i.handle(CheckoutInteractor.Input.OnAppear)
        i.handle(CheckoutInteractor.Input.UpdatePromoCode("OFF10"))
        i.handle(CheckoutInteractor.Input.ValidatePromo)
        assertEquals(CheckoutInteractor.PromoState.Valid(10), i.data.promoState)
        assertEquals(18000, i.data.subtotal)
        assertEquals(16200, i.data.total)   // applyPercent(18000, 10)
        assertEquals(1800, i.data.discount)
        assertFalse(i.data.promoSkipsDiscounted)
    }

    /**
     * Aurora viene con promo activa (17000 ya es el precio promo): un solo descuento
     * por producto — el código es válido pero no la toca, y la UI lo señala.
     */
    @Test fun promoDoesNotStackOnDiscountedLine() {
        val (i, _, checkout) = make(listOf(cartLine(SampleProducts.aurora)))
        checkout.promoResult = PromoResult(valid = true, code = "OFF10", percent = 10, error = null)
        i.handle(CheckoutInteractor.Input.OnAppear)
        i.handle(CheckoutInteractor.Input.UpdatePromoCode("OFF10"))
        i.handle(CheckoutInteractor.Input.ValidatePromo)
        assertEquals(CheckoutInteractor.PromoState.Valid(10), i.data.promoState)
        assertEquals(17000, i.data.subtotal)
        assertEquals(17000, i.data.total)   // sin apilar
        assertEquals(0, i.data.discount)
        assertTrue(i.data.promoSkipsDiscounted)
    }

    /** Un código validado viaja en el request de pago. */
    @Test fun payCarriesValidatedPromoCode() {
        val (i, _, checkout) = make(listOf(cartLine(SampleProducts.plain)))
        checkout.promoResult = PromoResult(valid = true, code = "OFF10", percent = 10, error = null)
        i.handle(CheckoutInteractor.Input.OnAppear)
        i.handle(CheckoutInteractor.Input.UpdatePromoCode("OFF10"))
        i.handle(CheckoutInteractor.Input.ValidatePromo)
        fillValid(i)
        i.handle(CheckoutInteractor.Input.Pay)
        assertEquals("OFF10", checkout.createdRequests.first().promoCode)
    }

    @Test fun freeShippingForSanMiguel() {
        val (i, _, _) = make()
        i.handle(CheckoutInteractor.Input.UpdateField(CheckoutField.Zip, "1663"))
        assertTrue(i.data.freeShipping)
        i.handle(CheckoutInteractor.Input.UpdateField(CheckoutField.Zip, "1000"))
        assertFalse(i.data.freeShipping)
    }

    @Test fun preferenceErrorReflectedInState() {
        val (i, _, checkout) = make()
        checkout.preferenceError = RuntimeException("Sin stock")
        fillValid(i)
        i.handle(CheckoutInteractor.Input.Pay)
        assertTrue(i.state is CheckoutInteractor.State.Error)
        assertNull(i.data.payment)
    }

    @Test fun formPrefillsFromSavedProfile() {
        val bus = EventBus()
        val cart = MockCartStore(bus, listOf(cartLine(SampleProducts.aurora)))
        val saved = CheckoutForm("Ana Pérez", "ana@mail.com", "1122334455", "Calle 123", "San Miguel", "Buenos Aires", "1663")
        val i = CheckoutInteractor(cart, MockCheckoutService(), profile = FakeCheckoutProfileStore(saved))
        assertEquals("Ana Pérez", i.data.form.name)
        assertEquals("1663", i.data.form.zip)
        assertTrue(i.data.freeShipping)
    }

    @Test fun payPersistsProfile() {
        val bus = EventBus()
        val cart = MockCartStore(bus, listOf(cartLine(SampleProducts.aurora)))
        val profile = FakeCheckoutProfileStore()
        val i = CheckoutInteractor(cart, MockCheckoutService(), profile = profile)
        fillValid(i)
        i.handle(CheckoutInteractor.Input.Pay)
        assertEquals(1, profile.savedForms.size)
        assertEquals("ana@mail.com", profile.stored?.email)
    }

    @Test fun failedPayDoesNotPersist() {
        val bus = EventBus()
        val cart = MockCartStore(bus, listOf(cartLine(SampleProducts.aurora)))
        val checkout = MockCheckoutService().apply { preferenceError = RuntimeException("Sin stock") }
        val profile = FakeCheckoutProfileStore()
        val i = CheckoutInteractor(cart, checkout, profile = profile)
        fillValid(i)
        i.handle(CheckoutInteractor.Input.Pay)
        assertTrue(profile.savedForms.isEmpty())
    }

    // MARK: vales

    private fun withVouchers(vouchers: List<com.serafino.domain.entities.loyalty.LoyaltyVoucher>, cartLines: List<CartLine> = listOf(cartLine(SampleProducts.aurora))): Pair<CheckoutInteractor, MockCheckoutService> {
        val bus = EventBus()
        val cart = MockCartStore(bus, cartLines)
        val checkout = MockCheckoutService()
        val loyalty = MockLoyaltyProvider(vouchersList = vouchers)
        return CheckoutInteractor(cart, checkout, loyalty = loyalty, catalog = MockProductCatalog()) to checkout
    }

    private fun cdd(productId: String, pct: Int = 10, cap: Int? = 3000) =
        SampleLoyalty.voucher("cdd", LoyaltyRewardType.CafeDelDia, format = null, productId = productId, pct = pct, cap = cap)

    @Test fun onlyActiveVouchersOffered() {
        val (i, _) = withVouchers(listOf(
            SampleLoyalty.voucher("v1", LoyaltyRewardType.FreeProduct, format = "250 g"),
            SampleLoyalty.voucher("v2", LoyaltyRewardType.FreeProduct, status = "redeemed"),
        ))
        i.handle(CheckoutInteractor.Input.OnAppear)
        assertEquals(listOf("v1"), i.data.vouchers.map { it.id })
    }

    @Test fun cafeDelDiaWarnsWhenCoffeeMissing() {
        val (i, _) = withVouchers(listOf(cdd("serafino-casa")))
        i.handle(CheckoutInteractor.Input.OnAppear)
        i.handle(CheckoutInteractor.Input.SelectVoucher("cdd"))
        assertNotNull(i.data.voucherWarning)
        assertTrue(i.data.voucherWarning!!.contains("Serafino Premium"))
        assertNull(i.data.voucherNote)
    }

    @Test fun cafeDelDiaOkWhenCoffeeInCart() {
        val (i, _) = withVouchers(listOf(cdd("aurora")))
        i.handle(CheckoutInteractor.Input.OnAppear)
        i.handle(CheckoutInteractor.Input.SelectVoucher("cdd"))
        assertNull(i.data.voucherWarning)
        assertNotNull(i.data.voucherNote)
    }

    @Test fun freeCoffeeVoucherCarriesProductId() {
        val (i, checkout) = withVouchers(listOf(SampleLoyalty.voucher("v1", LoyaltyRewardType.FreeProduct, format = "250 g")))
        i.handle(CheckoutInteractor.Input.OnAppear)
        fillValid(i)
        i.handle(CheckoutInteractor.Input.SelectVoucher("v1"))
        i.handle(CheckoutInteractor.Input.Pay)
        assertEquals(1, checkout.createdRequests.size)
        assertEquals("v1", checkout.createdRequests.first().voucher?.id)
        assertNotNull(checkout.createdRequests.first().voucher?.productId)
    }

    @Test fun cafeDelDiaPreviewsAndLocksPromo() {
        val (i, _) = withVouchers(listOf(cdd("aurora")), listOf(cartLine(SampleProducts.aurora)))
        i.handle(CheckoutInteractor.Input.OnAppear)
        i.handle(CheckoutInteractor.Input.SelectVoucher("cdd"))
        assertEquals(17000, i.data.subtotal)
        assertEquals(1700, i.data.discount)
        assertEquals(15300, i.data.total)
        assertTrue(i.data.promoLocked)
    }

    /** Café del Día + otro producto: el código aplica solo al resto del carrito. */
    @Test fun cafeDelDiaPlusOtherSplitsDiscount() {
        // Aurora (café del día, 17000) + Plano (18000, sin promo de catálogo).
        val (i, checkout) = withVouchers(listOf(cdd("aurora")), listOf(cartLine(SampleProducts.aurora), cartLine(SampleProducts.plain)))
        checkout.promoResult = PromoResult(valid = true, code = "OFF10", percent = 10, error = null)
        i.handle(CheckoutInteractor.Input.OnAppear)
        i.handle(CheckoutInteractor.Input.UpdatePromoCode("OFF10"))
        i.handle(CheckoutInteractor.Input.ValidatePromo)
        i.handle(CheckoutInteractor.Input.SelectVoucher("cdd"))
        assertFalse(i.data.promoLocked)
        assertEquals(CheckoutInteractor.PromoState.Valid(10), i.data.promoState)
        assertEquals(17000 + 18000, i.data.subtotal)
        // aurora: café del día 10% → 15300 (nunca el código). plano: código 10% → 16200.
        assertEquals(15300 + 16200, i.data.total)
        assertEquals(35000 - 31500, i.data.discount)
    }

    @Test fun selectingCafeDelDiaDropsAppliedPromo() {
        val (i, checkout) = withVouchers(listOf(cdd("aurora")), listOf(cartLine(SampleProducts.aurora)))
        checkout.promoResult = PromoResult(valid = true, code = "OFF10", percent = 10, error = null)
        i.handle(CheckoutInteractor.Input.OnAppear)
        i.handle(CheckoutInteractor.Input.UpdatePromoCode("OFF10"))
        i.handle(CheckoutInteractor.Input.ValidatePromo)
        assertEquals(CheckoutInteractor.PromoState.Valid(10), i.data.promoState)
        i.handle(CheckoutInteractor.Input.SelectVoucher("cdd"))
        assertTrue(i.data.promoLocked)
        assertEquals(CheckoutInteractor.PromoState.None, i.data.promoState)
        assertEquals(15300, i.data.total)

        // Y al pagar, el código soltado NO viaja al backend; el vale sí.
        fillValid(i)
        i.handle(CheckoutInteractor.Input.Pay)
        assertNull(checkout.createdRequests.first().promoCode)
        assertEquals("cdd", checkout.createdRequests.first().voucher?.id)
    }

    /** Deseleccionar el Café del Día devuelve su línea al precio de catálogo. */
    @Test fun deselectingCafeDelDiaRestoresPromo() {
        // pct 15 para que el descuento del café del día se distinga del código (10%).
        // Plano acompaña sin promo de catálogo: el código le aplica siempre.
        val (i, checkout) = withVouchers(listOf(cdd("aurora", pct = 15)), listOf(cartLine(SampleProducts.aurora), cartLine(SampleProducts.plain)))
        checkout.promoResult = PromoResult(valid = true, code = "OFF10", percent = 10, error = null)
        i.handle(CheckoutInteractor.Input.OnAppear)
        i.handle(CheckoutInteractor.Input.UpdatePromoCode("OFF10"))
        i.handle(CheckoutInteractor.Input.ValidatePromo)
        i.handle(CheckoutInteractor.Input.SelectVoucher("cdd"))
        // aurora: café del día 15% → 14450. plano: código 10% → 16200.
        assertEquals(14450 + 16200, i.data.total)
        i.handle(CheckoutInteractor.Input.SelectVoucher("cdd")) // deselecciona
        assertNull(i.data.selectedVoucherID)
        assertFalse(i.data.promoLocked)
        // aurora vuelve a su precio promo de catálogo (el código no se apila encima);
        // plano conserva el código: 18000 → 16200.
        assertEquals(17000 + 16200, i.data.total)
    }

    @Test fun freeCoffeeWithoutChoiceBlocksPay() {
        val (i, checkout) = withVouchers(listOf(SampleLoyalty.voucher("v1", LoyaltyRewardType.FreeProduct, format = "999 g")))
        i.handle(CheckoutInteractor.Input.OnAppear)
        fillValid(i)
        i.handle(CheckoutInteractor.Input.SelectVoucher("v1"))
        i.handle(CheckoutInteractor.Input.Pay)
        assertTrue(checkout.createdRequests.isEmpty())
        assertNotNull(i.data.voucherWarning)
    }

    /** Un canje con el Checkout ya montado recarga los vales (RewardRedeemedEvent). */
    @Test fun rewardRedeemedReloadsVouchers() {
        val bus = EventBus()
        val cart = MockCartStore(bus, listOf(cartLine(SampleProducts.aurora)))
        val loyalty = MockLoyaltyProvider(vouchersList = listOf(SampleLoyalty.voucher("v1")))
        val i = CheckoutInteractor(cart, MockCheckoutService(), loyalty = loyalty, catalog = MockProductCatalog(), bus = bus)
        i.handle(CheckoutInteractor.Input.OnAppear)
        assertEquals(listOf("v1"), i.data.vouchers.map { it.id })
        assertEquals(1, loyalty.vouchersCount)

        // El usuario canjea en otra pantalla: el Checkout se entera y muestra el vale nuevo.
        loyalty.vouchersList = listOf(
            SampleLoyalty.voucher("v1"),
            SampleLoyalty.voucher("v2", LoyaltyRewardType.Merch, format = null),
        )
        bus.publish(RewardRedeemedEvent())
        assertEquals(2, loyalty.vouchersCount)
        assertEquals(listOf("v1", "v2"), i.data.vouchers.map { it.id })
    }

    /** Si el vale seleccionado se consumió en otra pantalla, la recarga lo deselecciona. */
    @Test fun rewardRedeemedClearsStaleSelection() {
        val bus = EventBus()
        val cart = MockCartStore(bus, listOf(cartLine(SampleProducts.aurora)))
        val loyalty = MockLoyaltyProvider(vouchersList = listOf(SampleLoyalty.voucher("v1", LoyaltyRewardType.Merch, format = null)))
        val i = CheckoutInteractor(cart, MockCheckoutService(), loyalty = loyalty, catalog = MockProductCatalog(), bus = bus)
        i.handle(CheckoutInteractor.Input.OnAppear)
        i.handle(CheckoutInteractor.Input.SelectVoucher("v1"))
        assertEquals("v1", i.data.selectedVoucherID)
        assertNotNull(i.data.voucherNote)

        // El vale se consume en Redeem (pedido directo): el Checkout recarga la lista y
        // la selección obsoleta se limpia — el pedido no promete un premio que no va más.
        loyalty.vouchersList = emptyList()
        bus.publish(RewardRedeemedEvent())
        assertTrue(i.data.vouchers.isEmpty())
        assertNull(i.data.selectedVoucherID)
        assertNull(i.data.voucherNote)
    }
}

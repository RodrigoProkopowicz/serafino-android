package com.serafino.feature.store

import com.serafino.architecture.EventBus
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
        assertNotNull(i.data.payment)
        assertEquals("order-1", i.data.lastOrderId)
    }

    @Test fun promoPreviewRecomputesTotals() {
        val (i, _, checkout) = make()
        checkout.promoResult = PromoResult(valid = true, code = "OFF10", percent = 10, error = null)
        i.handle(CheckoutInteractor.Input.OnAppear)
        i.handle(CheckoutInteractor.Input.UpdatePromoCode("OFF10"))
        i.handle(CheckoutInteractor.Input.ValidatePromo)
        assertEquals(CheckoutInteractor.PromoState.Valid(10), i.data.promoState)
        assertEquals(17000, i.data.subtotal)
        assertEquals(15300, i.data.total)
        assertEquals(1700, i.data.discount)
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

    @Test fun cafeDelDiaPlusOtherSplitsDiscount() {
        val (i, checkout) = withVouchers(listOf(cdd("aurora")), listOf(cartLine(SampleProducts.aurora), cartLine(SampleProducts.premium)))
        checkout.promoResult = PromoResult(valid = true, code = "OFF10", percent = 10, error = null)
        i.handle(CheckoutInteractor.Input.OnAppear)
        i.handle(CheckoutInteractor.Input.UpdatePromoCode("OFF10"))
        i.handle(CheckoutInteractor.Input.ValidatePromo)
        i.handle(CheckoutInteractor.Input.SelectVoucher("cdd"))
        assertFalse(i.data.promoLocked)
        assertEquals(CheckoutInteractor.PromoState.Valid(10), i.data.promoState)
        assertEquals(17000 + 16150, i.data.subtotal)
        assertEquals(15300 + 14535, i.data.total)
        assertEquals(33150 - 29835, i.data.discount)
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
    }

    @Test fun deselectingCafeDelDiaRestoresPromo() {
        val (i, checkout) = withVouchers(listOf(cdd("aurora", pct = 15)), listOf(cartLine(SampleProducts.aurora), cartLine(SampleProducts.premium)))
        checkout.promoResult = PromoResult(valid = true, code = "OFF10", percent = 10, error = null)
        i.handle(CheckoutInteractor.Input.OnAppear)
        i.handle(CheckoutInteractor.Input.UpdatePromoCode("OFF10"))
        i.handle(CheckoutInteractor.Input.ValidatePromo)
        i.handle(CheckoutInteractor.Input.SelectVoucher("cdd"))
        assertEquals(14450 + 14535, i.data.total)
        i.handle(CheckoutInteractor.Input.SelectVoucher("cdd")) // deselecciona
        assertNull(i.data.selectedVoucherID)
        assertFalse(i.data.promoLocked)
        assertEquals(15300 + 14535, i.data.total)
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
}

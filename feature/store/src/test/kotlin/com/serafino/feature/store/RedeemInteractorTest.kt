package com.serafino.feature.store

import com.serafino.architecture.BeansEarnedEvent
import com.serafino.architecture.EventBus
import com.serafino.architecture.RewardRedeemedEvent
import com.serafino.architecture.events
import com.serafino.domain.entities.loyalty.LoyaltyAccount
import com.serafino.domain.entities.loyalty.LoyaltyReward
import com.serafino.domain.entities.loyalty.LoyaltyRewardType
import com.serafino.domain.entities.store.GrindOption
import com.serafino.domain.services.CheckoutField
import com.serafino.domain.services.CheckoutForm
import com.serafino.feature.store.redeem.RedeemInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Paridad con `RedeemInteractorTests` de iOS. */
class RedeemInteractorTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun make(
        account: LoyaltyAccount = SampleLoyalty.account(),
        vouchers: List<com.serafino.domain.entities.loyalty.LoyaltyVoucher> = emptyList(),
        profile: FakeCheckoutProfileStore? = null,
    ): Triple<RedeemInteractor, MockLoyaltyProvider, EventBus> {
        val bus = EventBus()
        val loyalty = MockLoyaltyProvider(account = account, vouchersList = vouchers)
        val interactor = RedeemInteractor(loyalty, bus, catalog = MockProductCatalog(), profile = profile)
        return Triple(interactor, loyalty, bus)
    }

    private fun fillValidShipping(i: RedeemInteractor, name: String = "Ana Pérez") {
        i.handle(RedeemInteractor.Input.UpdateShippingField(CheckoutField.Name, name))
        i.handle(RedeemInteractor.Input.UpdateShippingField(CheckoutField.Email, "ana@mail.com"))
        i.handle(RedeemInteractor.Input.UpdateShippingField(CheckoutField.Phone, "1122334455"))
        i.handle(RedeemInteractor.Input.UpdateShippingField(CheckoutField.Street, "Av. Mitre 123"))
        i.handle(RedeemInteractor.Input.UpdateShippingField(CheckoutField.City, "San Miguel"))
        i.handle(RedeemInteractor.Input.UpdateShippingField(CheckoutField.Province, "Buenos Aires"))
        i.handle(RedeemInteractor.Input.UpdateShippingField(CheckoutField.Zip, "1663"))
    }

    @Test fun loadsCatalogAndVouchers() {
        val (i, _, _) = make(vouchers = listOf(
            SampleLoyalty.voucher("v1", LoyaltyRewardType.FreeProduct, format = "250 g"),
            SampleLoyalty.voucher("v2", LoyaltyRewardType.Merch, format = null),
            SampleLoyalty.voucher("v3", LoyaltyRewardType.FreeProduct, status = "redeemed"),
        ))
        i.handle(RedeemInteractor.Input.OnAppear)
        assertEquals(RedeemInteractor.State.Loaded, i.state)
        assertEquals("granos", i.data.pointsName)
        assertTrue(i.data.freeRewards.isNotEmpty())
        assertTrue(i.data.merchRewards.isNotEmpty())
        assertEquals("cafe-del-dia", i.data.cafe?.redeemId)
        assertEquals("Aurora", i.data.cafe?.coffeeName)
        assertEquals(listOf("v1", "v2"), i.data.vouchers.map { it.id })
        assertTrue(i.data.vouchers.first().expiryText!!.contains("Vence"))
        assertTrue(i.data.vouchers.first { it.id == "v1" }.canShip)
    }

    @Test fun cafeDelDiaResolvesCoffeeName() {
        val (i, _, _) = make()
        i.handle(RedeemInteractor.Input.OnAppear)
        assertEquals("Aurora", i.data.cafe?.coffeeName)
        assertEquals("10% de descuento", i.data.cafe?.discountText)
    }

    @Test fun needMoreShowsShortfall() {
        val reward = LoyaltyReward("free-500", LoyaltyRewardType.FreeProduct, 700, "700 granos", "top", "Café Rendidor 500 g", "500 g", unlocked = true, affordable = false, canRedeem = false)
        val account = LoyaltyAccount.make(points = 450, lifetimePoints = 800, amountPerPoint = 1000, tiers = SampleLoyalty.tiers, rewards = listOf(reward), history = emptyList())
        val (i, _, _) = make(account = account)
        i.handle(RedeemInteractor.Input.OnAppear)
        assertEquals(RedeemInteractor.Availability.NeedMore("Te faltan 250 granos"), i.data.freeRewards.first().availability)
    }

    @Test fun redeemSendsIdempotencyKeyAndRefreshes() {
        val (i, loyalty, bus) = make()
        val earned = mutableListOf<Int>()
        val scope = CoroutineScope(mainRule.dispatcher)
        scope.launch { bus.events<BeansEarnedEvent>().collect { earned.add(it.amount) } }
        val redeems = eventProbe<RewardRedeemedEvent>(bus)
        i.handle(RedeemInteractor.Input.OnAppear)
        i.handle(RedeemInteractor.Input.Redeem("free-125"))
        assertEquals(1, loyalty.redeemCalls.size)
        assertEquals("free-125", loyalty.redeemCalls.first().first)
        assertTrue(loyalty.redeemCalls.first().second!!.isNotEmpty())
        assertNotNull(i.data.resultMessage)
        assertEquals(listOf(-250), earned)
        // Hay un vale nuevo: el Checkout y el Perfil se enteran por este evento.
        assertEquals(1, redeems.events.size)
        scope.cancel()
    }

    /**
     * Un segundo canje exitoso del mismo premio usa una clave de idempotencia nueva —
     * reusar la anterior haría que el backend devuelva el vale viejo sin debitar.
     */
    @Test fun secondSuccessfulRedeemUsesFreshKey() {
        val (i, loyalty, _) = make()
        i.handle(RedeemInteractor.Input.OnAppear)
        i.handle(RedeemInteractor.Input.Redeem("free-125"))
        assertEquals(1, loyalty.redeemCalls.size)
        i.handle(RedeemInteractor.Input.Redeem("free-125"))
        assertEquals(2, loyalty.redeemCalls.size)
        val first = loyalty.redeemCalls[0].second
        val second = loyalty.redeemCalls[1].second
        assertNotNull(first)
        assertNotNull(second)
        assertTrue(first != second)
    }

    @Test fun idempotencyKeyStableAcrossRetries() {
        val (i, loyalty, _) = make()
        i.handle(RedeemInteractor.Input.OnAppear)
        loyalty.redeemError = RuntimeException("Sin red")
        i.handle(RedeemInteractor.Input.Redeem("free-125"))
        assertNotNull(i.data.errorMessage)
        loyalty.redeemError = null
        i.handle(RedeemInteractor.Input.ClearError)
        i.handle(RedeemInteractor.Input.Redeem("free-125"))
        assertEquals(2, loyalty.redeemCalls.size)
        val k1 = loyalty.redeemCalls[0].second
        val k2 = loyalty.redeemCalls[1].second
        assertNotNull(k1)
        assertEquals(k1, k2)
        i.handle(RedeemInteractor.Input.Redeem("merch-tote"))
        assertEquals(3, loyalty.redeemCalls.size)
        assertTrue(loyalty.redeemCalls[2].second != k1)
    }

    @Test fun redeemDirectOpensShippingForm() {
        val (i, loyalty, _) = make(vouchers = listOf(SampleLoyalty.voucher("v1", LoyaltyRewardType.FreeProduct, format = "250 g")))
        i.handle(RedeemInteractor.Input.OnAppear)
        i.handle(RedeemInteractor.Input.RedeemDirect("v1"))
        assertEquals("v1", i.data.shippingVoucher?.id)
        assertTrue(loyalty.directCalls.isEmpty())
    }

    @Test fun confirmShippingValidatesRequiredFields() {
        val (i, loyalty, _) = make(vouchers = listOf(SampleLoyalty.voucher("v1", LoyaltyRewardType.FreeProduct, format = "250 g")))
        i.handle(RedeemInteractor.Input.OnAppear)
        i.handle(RedeemInteractor.Input.RedeemDirect("v1"))
        i.handle(RedeemInteractor.Input.ConfirmShipping)
        assertTrue(loyalty.directCalls.isEmpty())
        listOf(CheckoutField.Name, CheckoutField.Email, CheckoutField.Phone, CheckoutField.Street, CheckoutField.City, CheckoutField.Province, CheckoutField.Zip)
            .forEach { assertNotNull(i.data.shippingErrors[it]) }
        assertEquals("v1", i.data.shippingVoucher?.id)
    }

    @Test fun directRedeemShipsVoucherWithContact() {
        val (i, loyalty, _) = make(vouchers = listOf(SampleLoyalty.voucher("v1", LoyaltyRewardType.FreeProduct, format = "250 g")))
        i.handle(RedeemInteractor.Input.OnAppear)
        i.handle(RedeemInteractor.Input.SelectShipCoffee("v1", "aurora"))
        i.handle(RedeemInteractor.Input.RedeemDirect("v1"))
        i.handle(RedeemInteractor.Input.SelectShippingGrind(GrindOption.Media))
        fillValidShipping(i, name = "  Ana Pérez  ")
        i.handle(RedeemInteractor.Input.ConfirmShipping)
        assertEquals(1, loyalty.directCalls.size)
        val call = loyalty.directCalls.first()
        assertEquals("v1", call.voucherId)
        assertEquals("aurora", call.productId)
        assertEquals("media", call.grind)
        assertEquals("Ana Pérez", call.contact.name)
        assertEquals("ana@mail.com", call.contact.email)
        assertEquals("1663", call.contact.zip)
        assertNull(i.data.shippingVoucher)
        assertTrue(i.data.resultMessage!!.contains("envío"))
    }

    /** El canje directo con envío publica RewardRedeemedEvent (se consumió un vale). */
    @Test fun directRedeemPublishesRewardRedeemed() {
        val (i, _, bus) = make(vouchers = listOf(SampleLoyalty.voucher("v1", LoyaltyRewardType.FreeProduct, format = "250 g")))
        val redeems = eventProbe<RewardRedeemedEvent>(bus)
        i.handle(RedeemInteractor.Input.OnAppear)
        i.handle(RedeemInteractor.Input.RedeemDirect("v1"))
        fillValidShipping(i)
        i.handle(RedeemInteractor.Input.ConfirmShipping)
        assertEquals(1, redeems.events.size)
    }

    @Test fun directRedeemMerchHasNoGrind() {
        val (i, loyalty, _) = make(vouchers = listOf(SampleLoyalty.voucher("m1", LoyaltyRewardType.Merch, format = null)))
        i.handle(RedeemInteractor.Input.OnAppear)
        i.handle(RedeemInteractor.Input.RedeemDirect("m1"))
        i.handle(RedeemInteractor.Input.SelectShippingGrind(GrindOption.Fina))
        fillValidShipping(i)
        i.handle(RedeemInteractor.Input.ConfirmShipping)
        assertEquals(1, loyalty.directCalls.size)
        assertEquals("m1", loyalty.directCalls.first().voucherId)
        assertNull(loyalty.directCalls.first().productId)
        assertNull(loyalty.directCalls.first().grind)
    }

    @Test fun shippingFormPrefillsFromSavedProfile() {
        val saved = CheckoutForm(name = "Ana Pérez", street = "Mitre 123", city = "San Miguel", province = "Buenos Aires", zip = "1663")
        val (i, _, _) = make(vouchers = listOf(SampleLoyalty.voucher("v1", LoyaltyRewardType.FreeProduct, format = "250 g")), profile = FakeCheckoutProfileStore(saved))
        i.handle(RedeemInteractor.Input.OnAppear)
        i.handle(RedeemInteractor.Input.RedeemDirect("v1"))
        assertEquals("Ana Pérez", i.data.shippingForm.name)
        assertEquals("1663", i.data.shippingForm.zip)
    }

    @Test fun confirmShippingPersistsProfile() {
        val profile = FakeCheckoutProfileStore()
        val (i, _, _) = make(vouchers = listOf(SampleLoyalty.voucher("v1", LoyaltyRewardType.FreeProduct, format = "250 g")), profile = profile)
        i.handle(RedeemInteractor.Input.OnAppear)
        i.handle(RedeemInteractor.Input.RedeemDirect("v1"))
        fillValidShipping(i, name = "Ana")
        i.handle(RedeemInteractor.Input.ConfirmShipping)
        assertEquals(1, profile.savedForms.size)
        assertEquals("Ana", profile.stored?.name)
        assertEquals("1663", profile.stored?.zip)
    }

    @Test fun directRedeemErrorKeepsSheetOpen() {
        val (i, loyalty, _) = make(vouchers = listOf(SampleLoyalty.voucher("v1", LoyaltyRewardType.FreeProduct, format = "250 g")))
        loyalty.directError = RuntimeException("El vale venció")
        i.handle(RedeemInteractor.Input.OnAppear)
        i.handle(RedeemInteractor.Input.RedeemDirect("v1"))
        fillValidShipping(i)
        i.handle(RedeemInteractor.Input.ConfirmShipping)
        assertEquals("El vale venció", i.data.shippingSubmitError)
        assertEquals("v1", i.data.shippingVoucher?.id)
        assertNull(i.data.shippingID)
    }

    @Test fun redeemErrorSurfaces() {
        val (i, loyalty, _) = make()
        loyalty.redeemError = RuntimeException("No tenés saldo")
        i.handle(RedeemInteractor.Input.OnAppear)
        i.handle(RedeemInteractor.Input.Redeem("free-125"))
        assertEquals("No tenés saldo", i.data.errorMessage)
        assertNull(i.data.redeemingID)
        assertEquals(RedeemInteractor.State.Loaded, i.state)
    }
}

package com.serafino.feature.store.checkout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.serafino.architecture.AnalyticsEvent
import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.Interactor
import com.serafino.architecture.NoOpAnalytics
import com.serafino.domain.entities.loyalty.LoyaltyRewardType
import com.serafino.domain.entities.loyalty.LoyaltyVoucher
import com.serafino.domain.entities.store.GrindOption
import com.serafino.domain.entities.store.Product
import com.serafino.domain.services.CartStoring
import com.serafino.domain.services.CheckoutForm
import com.serafino.domain.services.CheckoutProfileStoring
import com.serafino.domain.services.CheckoutRequest
import com.serafino.domain.services.CheckoutService
import com.serafino.domain.services.CheckoutField
import com.serafino.domain.services.CheckoutValidator
import com.serafino.domain.services.LoyaltyProviding
import com.serafino.domain.services.OrderPricing
import com.serafino.domain.services.ProductCatalogProviding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Link de pago de Mercado Pago a presentar. */
data class PaymentLink(val orderId: String, val url: String)

/**
 * Arma el pedido: valida el formulario, previsualiza el promo, calcula totales (fórmula del
 * backend) y crea la preferencia de Mercado Pago. Espeja `CheckoutInteractor` de iOS.
 */
class CheckoutInteractor(
    private val cart: CartStoring,
    private val checkout: CheckoutService,
    private val analytics: AnalyticsTracking = NoOpAnalytics(),
    private val loyalty: LoyaltyProviding? = null,
    private val catalog: ProductCatalogProviding? = null,
    private val profile: CheckoutProfileStoring? = null,
) : Interactor<CheckoutInteractor.Data, CheckoutInteractor.Input, CheckoutInteractor.State> {

    sealed interface PromoState {
        data object None : PromoState
        data object Validating : PromoState
        data class Valid(val percent: Int) : PromoState
        data class Invalid(val message: String) : PromoState

        val percentIfValid: Int? get() = (this as? Valid)?.percent
    }

    data class CoffeeOption(val id: String, val name: String)

    data class Data(
        val form: CheckoutForm = CheckoutForm(),
        val grind: GrindOption = GrindOption.Default,
        val promoCode: String = "",
        val promoState: PromoState = PromoState.None,
        val fieldErrors: Map<CheckoutField, String> = emptyMap(),
        val subtotal: Int = 0,
        val discount: Int = 0,
        val total: Int = 0,
        val freeShipping: Boolean = false,
        val promoLocked: Boolean = false,
        val notice: String? = null,
        val itemCount: Int = 0,
        val payment: PaymentLink? = null,
        val lastOrderId: String? = null,
        val vouchers: List<LoyaltyVoucher> = emptyList(),
        val selectedVoucherID: String? = null,
        val freeCoffeeOptions: List<CoffeeOption> = emptyList(),
        val selectedFreeCoffeeID: String? = null,
        val voucherWarning: String? = null,
        val voucherNote: String? = null,
    )

    sealed interface Input {
        data object OnAppear : Input
        data class UpdateField(val field: CheckoutField, val value: String) : Input
        data class SelectGrind(val grind: GrindOption) : Input
        data class UpdatePromoCode(val code: String) : Input
        data object ValidatePromo : Input
        data class SelectVoucher(val id: String?) : Input
        data class SelectFreeCoffee(val id: String) : Input
        data object Pay : Input
        data object ClearPayment : Input
    }

    sealed interface State {
        data object Editing : State
        data object CreatingPreference : State
        data class Error(val message: String) : State
    }

    override var data by mutableStateOf(Data())
        private set
    override var state by mutableStateOf<State>(State.Editing)
        private set

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var allProducts: List<Product> = emptyList()

    init {
        profile?.load()?.let { saved ->
            data = data.copy(form = saved, freeShipping = CheckoutValidator.isSanMiguel(saved.city, saved.zip))
        }
    }

    override fun handle(input: Input) {
        when (input) {
            is Input.OnAppear -> {
                data = data.copy(itemCount = cart.count)
                recomputeTotals()
                loadNotice()
                loadVouchers()
                analytics.track(AnalyticsEvent.BeginCheckout(data.total, data.itemCount))
            }
            is Input.UpdateField -> {
                var form = data.form.with(input.field, input.value)
                val errors = data.fieldErrors - input.field
                var free = data.freeShipping
                if (input.field == CheckoutField.City || input.field == CheckoutField.Zip) {
                    free = CheckoutValidator.isSanMiguel(form.city, form.zip)
                }
                data = data.copy(form = form, fieldErrors = errors, freeShipping = free)
            }
            is Input.SelectGrind -> data = data.copy(grind = input.grind)
            is Input.UpdatePromoCode -> {
                data = data.copy(promoCode = input.code, promoState = PromoState.None)
                recomputeTotals()
            }
            is Input.ValidatePromo -> validatePromo()
            is Input.SelectVoucher -> selectVoucher(input.id)
            is Input.SelectFreeCoffee -> data = data.copy(selectedFreeCoffeeID = input.id)
            is Input.Pay -> pay()
            is Input.ClearPayment -> data = data.copy(payment = null)
        }
    }

    fun dispose() = scope.cancel()

    private fun recomputeTotals() {
        val cafeDelDia = activeCafeDelDia()
        val coversWholeCart = cafeDelDia?.let { cafe -> cart.lines.none { it.productID != cafe.productID } } ?: false
        var promoState = data.promoState
        if (coversWholeCart && promoState.percentIfValid != null) promoState = PromoState.None
        val percent = if (coversWholeCart) null else promoState.percentIfValid
        val totals = OrderPricing.preview(
            lines = cart.lines.map { OrderPricing.Line(it.productID, it.unitPrice, it.quantity) },
            promoPercent = percent,
            cafeDelDia = cafeDelDia,
        )
        data = data.copy(
            promoLocked = coversWholeCart,
            promoState = promoState,
            subtotal = totals.subtotal,
            discount = totals.discount,
            total = totals.total,
        )
    }

    private fun activeCafeDelDia(): OrderPricing.CafeDelDia? {
        val id = data.selectedVoucherID ?: return null
        val voucher = data.vouchers.firstOrNull { it.id == id } ?: return null
        if (voucher.type != LoyaltyRewardType.CafeDelDia) return null
        val productId = voucher.productId ?: return null
        if (cart.lines.none { it.productID == productId }) return null
        return OrderPricing.CafeDelDia(productID = productId, pct = voucher.pct ?: 0, maxDiscountARS = voucher.maxDiscountARS)
    }

    private fun loadNotice() {
        if (data.notice != null) return
        scope.launch {
            val notice = checkout.checkoutNotice()
            if (notice != null && notice.enabled && notice.text.isNotEmpty()) {
                data = data.copy(notice = notice.text)
            }
        }
    }

    private fun validatePromo() {
        if (data.promoLocked) return
        val code = data.promoCode.trim()
        if (code.isEmpty()) {
            data = data.copy(promoState = PromoState.Invalid("Ingresá un código."))
            return
        }
        data = data.copy(promoState = PromoState.Validating)
        scope.launch {
            val result = checkout.validatePromo(code)
            data = if (result.valid && result.percent != null) {
                data.copy(promoState = PromoState.Valid(result.percent!!))
            } else {
                data.copy(promoState = PromoState.Invalid(result.error ?: "Código inválido o vencido."))
            }
            recomputeTotals()
        }
    }

    private fun loadVouchers() {
        val loyalty = loyalty ?: return
        scope.launch {
            val active = runCatching { loyalty.vouchers() }.getOrDefault(emptyList()).filter { it.isActive }
            data = data.copy(vouchers = active)
            if (active.any { it.type == LoyaltyRewardType.FreeProduct || it.type == LoyaltyRewardType.CafeDelDia } && catalog != null) {
                allProducts = runCatching { catalog.loadProducts() }.getOrDefault(emptyList())
            }
        }
    }

    private fun selectVoucher(id: String?) {
        // Tocar el vale ya seleccionado lo deselecciona.
        if (id == null || id == data.selectedVoucherID) {
            data = data.copy(selectedVoucherID = null, freeCoffeeOptions = emptyList(), selectedFreeCoffeeID = null, voucherWarning = null, voucherNote = null)
            recomputeTotals()
            return
        }
        val voucher = data.vouchers.firstOrNull { it.id == id }
        if (voucher == null) {
            data = data.copy(selectedVoucherID = id, freeCoffeeOptions = emptyList(), selectedFreeCoffeeID = null, voucherWarning = null, voucherNote = null)
            recomputeTotals()
            return
        }
        var options = emptyList<CoffeeOption>()
        var selectedCoffee: String? = null
        var warning: String? = null
        var note: String? = null
        when (voucher.type) {
            LoyaltyRewardType.FreeProduct -> {
                options = voucher.format?.let { format ->
                    allProducts.filter { p -> p.formats.any { it.weight == format } }.map { CoffeeOption(it.id, it.name) }
                } ?: emptyList()
                selectedCoffee = options.firstOrNull()?.id
                if (options.isEmpty()) warning = "No encontramos un café del formato de este vale para sumar."
                else note = "Tu café gratis se suma al pedido sin costo en el pago."
            }
            LoyaltyRewardType.CafeDelDia -> {
                val productId = voucher.productId
                if (productId != null && cart.lines.none { it.productID == productId }) {
                    val name = allProducts.firstOrNull { it.id == productId }?.name ?: "el café de hoy"
                    warning = "Agregá $name al carrito para aplicar el descuento del Café del Día."
                } else {
                    note = "El descuento del Café del Día ya está aplicado en el total (solo sobre ese café)."
                }
            }
            LoyaltyRewardType.Merch -> note = "Tu merch se suma al pedido sin costo en el pago."
            is LoyaltyRewardType.Other -> {}
        }
        data = data.copy(
            selectedVoucherID = id,
            freeCoffeeOptions = options,
            selectedFreeCoffeeID = selectedCoffee,
            voucherWarning = warning,
            voucherNote = note,
        )
        recomputeTotals()
    }

    private fun selectedVoucherRequest(): CheckoutRequest.Voucher? {
        val id = data.selectedVoucherID ?: return null
        val voucher = data.vouchers.firstOrNull { it.id == id } ?: return null
        if (voucher.type == LoyaltyRewardType.FreeProduct) {
            val coffeeID = data.selectedFreeCoffeeID ?: return null
            return CheckoutRequest.Voucher(voucher.id, coffeeID)
        }
        return CheckoutRequest.Voucher(voucher.id, null)
    }

    private fun pay() {
        val errors = CheckoutValidator.validate(data.form)
        if (errors.isNotEmpty()) {
            data = data.copy(fieldErrors = errors)
            return
        }
        if (cart.lines.isEmpty()) {
            state = State.Error("Tu carrito está vacío.")
            return
        }
        val selId = data.selectedVoucherID
        if (selId != null) {
            val voucher = data.vouchers.firstOrNull { it.id == selId }
            if (voucher?.type == LoyaltyRewardType.FreeProduct && data.selectedFreeCoffeeID == null) {
                data = data.copy(voucherWarning = "Elegí qué café gratis querés sumar, o quitá el vale.")
                return
            }
        }
        state = State.CreatingPreference
        val request = buildRequest()
        scope.launch {
            runCatching { checkout.createPreference(request) }
                .onSuccess { preference ->
                    profile?.save(data.form.trimmed())
                    data = data.copy(lastOrderId = preference.orderId, payment = PaymentLink(preference.orderId, preference.initPoint))
                    state = State.Editing
                }
                .onFailure { state = State.Error(it.message ?: "No se pudo crear el pago.") }
        }
    }

    private fun buildRequest(): CheckoutRequest {
        val form = data.form.trimmed()
        val usePromo = data.promoState.percentIfValid != null
        return CheckoutRequest(
            items = cart.lines.map { CheckoutRequest.Item(it.productID, it.formatWeight, it.quantity) },
            payer = CheckoutRequest.Payer(form.name, form.email, form.phone),
            shipping = CheckoutRequest.Shipping(form.street, form.city, form.province, form.zip),
            promoCode = if (usePromo) data.promoCode.trim() else null,
            grind = data.grind.key,
            voucher = selectedVoucherRequest(),
        )
    }
}

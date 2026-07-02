package com.serafino.feature.store.redeem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.serafino.architecture.AnalyticsEvent
import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.BeansEarnedEvent
import com.serafino.architecture.RewardRedeemedEvent
import com.serafino.architecture.EventBus
import com.serafino.architecture.Interactor
import com.serafino.architecture.NoOpAnalytics
import com.serafino.designsystem.Haptics
import com.serafino.domain.entities.loyalty.CafeDelDia
import com.serafino.domain.entities.loyalty.LoyaltyAccount
import com.serafino.domain.entities.loyalty.LoyaltyReward
import com.serafino.domain.entities.loyalty.LoyaltyRewardType
import com.serafino.domain.entities.loyalty.LoyaltyVoucher
import com.serafino.domain.entities.loyalty.RedeemOutcome
import com.serafino.domain.entities.store.GrindOption
import com.serafino.domain.entities.store.Product
import com.serafino.domain.services.CheckoutField
import com.serafino.domain.services.CheckoutForm
import com.serafino.domain.services.CheckoutProfileStoring
import com.serafino.domain.services.CheckoutValidator
import com.serafino.domain.services.LoyaltyProviding
import com.serafino.domain.services.Money
import com.serafino.domain.services.ProductCatalogProviding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

/**
 * Pantalla "Canjear granos": catálogo de canje, Café del Día y vales del usuario. Canjea premios
 * por granos y pide vales directo con envío gratis. Espeja `RedeemInteractor` de iOS.
 */
class RedeemInteractor(
    private val loyalty: LoyaltyProviding,
    private val bus: EventBus,
    private val analytics: AnalyticsTracking = NoOpAnalytics(),
    private val catalog: ProductCatalogProviding? = null,
    private val profile: CheckoutProfileStoring? = null,
) : Interactor<RedeemInteractor.Data, RedeemInteractor.Input, RedeemInteractor.State> {

    sealed interface Availability {
        data object Redeemable : Availability
        data class Locked(val text: String) : Availability
        data class NeedMore(val text: String) : Availability
    }

    data class RewardRow(
        val id: String,
        val label: String,
        val sublabel: String,
        val costLabel: String,
        val symbol: String,
        val availability: Availability,
    )

    data class CafeRow(
        val redeemId: String,
        val dayLabel: String,
        val coffeeName: String?,
        val discountText: String,
        val capText: String?,
        val costLabel: String,
        val canRedeem: Boolean,
        val statusText: String?,
    )

    data class CoffeeOption(val id: String, val name: String)

    data class VoucherRow(
        val id: String,
        val label: String,
        val detail: String,
        val symbol: String,
        val expiryText: String?,
        val canShip: Boolean,
        val isCoffee: Boolean,
        val coffeeOptions: List<CoffeeOption>,
    )

    data class Data(
        val pointsName: String = "granos",
        val balanceText: String = "0",
        val freeRewards: List<RewardRow> = emptyList(),
        val merchRewards: List<RewardRow> = emptyList(),
        val cafe: CafeRow? = null,
        val vouchers: List<VoucherRow> = emptyList(),
        val shipCoffeeByVoucher: Map<String, String> = emptyMap(),
        val shippingGrind: GrindOption = GrindOption.Default,
        val redeemingID: String? = null,
        val shippingID: String? = null,
        val shippingVoucher: VoucherRow? = null,
        val shippingForm: CheckoutForm = CheckoutForm(),
        val shippingErrors: Map<CheckoutField, String> = emptyMap(),
        val shippingSubmitError: String? = null,
        val resultMessage: String? = null,
        val errorMessage: String? = null,
    )

    sealed interface Input {
        data object OnAppear : Input
        data object Retry : Input
        data class Redeem(val id: String) : Input
        data class SelectShipCoffee(val voucherID: String, val productID: String) : Input
        data class RedeemDirect(val id: String) : Input
        data class UpdateShippingField(val field: CheckoutField, val value: String) : Input
        data class SelectShippingGrind(val grind: GrindOption) : Input
        data object ConfirmShipping : Input
        data object CancelShipping : Input
        data object ClearResult : Input
        data object ClearError : Input
    }

    sealed interface State {
        data object Loading : State
        data object Loaded : State
        data class Error(val message: String) : State
    }

    override var data by mutableStateOf(Data())
        private set
    override var state by mutableStateOf<State>(State.Loading)
        private set

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var tierLabels: Map<String, String> = emptyMap()
    private var balance = 0
    private var allProducts: List<Product> = emptyList()
    private val idempotencyKeys = mutableMapOf<String, String>()

    override fun handle(input: Input) {
        when (input) {
            is Input.OnAppear -> {
                analytics.track(AnalyticsEvent.Screen("loyalty_redeem"))
                if (state !is State.Loaded) load()
            }
            is Input.Retry -> load()
            is Input.Redeem -> redeem(input.id)
            is Input.SelectShipCoffee -> data = data.copy(shipCoffeeByVoucher = data.shipCoffeeByVoucher + (input.voucherID to input.productID))
            is Input.RedeemDirect -> beginShipping(input.id)
            is Input.UpdateShippingField -> data = data.copy(
                shippingForm = data.shippingForm.with(input.field, input.value),
                shippingErrors = data.shippingErrors - input.field,
            )
            is Input.SelectShippingGrind -> data = data.copy(shippingGrind = input.grind)
            is Input.ConfirmShipping -> confirmShipping()
            is Input.CancelShipping -> cancelShipping()
            is Input.ClearResult -> data = data.copy(resultMessage = null)
            is Input.ClearError -> data = data.copy(errorMessage = null)
        }
    }

    fun dispose() = scope.cancel()

    private fun load() {
        state = State.Loading
        scope.launch { performLoad() }
    }

    private suspend fun performLoad() {
        val accountResult = runCatching {
            coroutineScope {
                val account = async { loyalty.loadAccount() }
                val vouchers = async { runCatching { loyalty.vouchers() }.getOrDefault(emptyList()) }
                account.await() to vouchers.await()
            }
        }
        accountResult
            .onSuccess { (account, vouchers) -> build(account, vouchers); state = State.Loaded }
            .onFailure { error ->
                if (data.freeRewards.isEmpty() && data.merchRewards.isEmpty()) {
                    state = State.Error(error.message ?: "No se pudo cargar.")
                }
            }
    }

    private fun build(account: LoyaltyAccount, vouchers: List<LoyaltyVoucher>) {
        balance = account.points
        tierLabels = account.sortedTiers.associate { it.id to it.name }
        data = data.copy(
            pointsName = account.pointsName,
            balanceText = grouped(account.points),
            freeRewards = account.rewards.filter { it.type == LoyaltyRewardType.FreeProduct }.map { row(it) },
            merchRewards = account.rewards.filter { it.type == LoyaltyRewardType.Merch }.map { row(it) },
        )
        val active = vouchers.filter { it.isUsable() }
        buildCatalogBacked(account.cafeDelDia, active)

        val needsCatalog = account.cafeDelDia?.productId != null || active.any { it.type == LoyaltyRewardType.FreeProduct }
        if (needsCatalog && allProducts.isEmpty() && catalog != null) {
            scope.launch {
                allProducts = runCatching { catalog.loadProducts() }.getOrDefault(emptyList())
                buildCatalogBacked(account.cafeDelDia, active)
            }
        }
    }

    private fun buildCatalogBacked(cafe: CafeDelDia?, vouchers: List<LoyaltyVoucher>) {
        val rows = vouchers.map { voucherRow(it) }
        // Preselección del café para vales de café gratis (si todavía no se eligió uno). Se calcula
        // aparte y se asigna en UN solo `copy`: mutar `data` dentro del `map` la perdería (el
        // receptor `data` se lee antes de que el map lo modifique).
        val preselect = data.shipCoffeeByVoucher.toMutableMap()
        rows.forEach { row ->
            if (row.isCoffee && preselect[row.id] == null) {
                row.coffeeOptions.firstOrNull()?.let { preselect[row.id] = it.id }
            }
        }
        data = data.copy(
            cafe = cafe?.let { cafeRow(it) },
            vouchers = rows,
            shipCoffeeByVoucher = preselect,
        )
    }

    private fun voucherRow(voucher: LoyaltyVoucher): VoucherRow {
        val options = if (voucher.type == LoyaltyRewardType.FreeProduct && voucher.format != null) {
            allProducts.filter { p -> p.formats.any { it.weight == voucher.format } }.map { CoffeeOption(it.id, it.name) }
        } else emptyList()
        val detail = when (voucher.type) {
            LoyaltyRewardType.FreeProduct -> voucher.format?.let { "Café gratis · $it" } ?: "Café gratis"
            LoyaltyRewardType.Merch -> "Merch"
            LoyaltyRewardType.CafeDelDia -> "Descuento del Café del Día"
            is LoyaltyRewardType.Other -> "Premio"
        }
        val canShip = voucher.type == LoyaltyRewardType.FreeProduct || voucher.type == LoyaltyRewardType.Merch
        return VoucherRow(
            id = voucher.id,
            label = voucher.label,
            detail = detail,
            symbol = voucher.type.symbolName,
            expiryText = voucher.expiresAt?.let { "Vence el ${dayMonth.format(it)}" },
            canShip = canShip,
            isCoffee = voucher.type == LoyaltyRewardType.FreeProduct,
            coffeeOptions = options,
        )
    }

    private fun row(reward: LoyaltyReward): RewardRow = RewardRow(
        id = reward.id,
        label = reward.label,
        sublabel = reward.format?.let { "Café · $it" } ?: typeLabel(reward.type),
        costLabel = reward.costLabel,
        symbol = reward.type.symbolName,
        availability = availability(reward),
    )

    private fun availability(reward: LoyaltyReward): Availability {
        if (reward.canRedeem) return Availability.Redeemable
        if (!reward.unlocked) {
            val name = tierLabels[reward.unlockTierKey] ?: reward.unlockTierKey
            return Availability.Locked("Subí a $name")
        }
        val missing = max(0, reward.cost - balance)
        return Availability.NeedMore("Te faltan ${grouped(missing)} ${data.pointsName}")
    }

    private fun cafeRow(cafe: CafeDelDia): CafeRow {
        val coffeeName = cafe.productId?.let { id -> allProducts.firstOrNull { it.id == id }?.name }
        val status = when {
            !cafe.affordable -> "Necesitás ${cafe.costLabel}"
            cafe.productId == null -> "Hoy no hay café del día"
            else -> null
        }
        return CafeRow(
            redeemId = cafe.redeemId,
            dayLabel = cafe.dayLabel,
            coffeeName = coffeeName,
            discountText = "${cafe.pct}% de descuento",
            capText = cafe.maxDiscountARS?.let { "Tope ${Money.ars(it)}" },
            costLabel = cafe.costLabel,
            canRedeem = cafe.affordable && cafe.productId != null,
            statusText = status,
        )
    }

    private fun typeLabel(type: LoyaltyRewardType): String = when (type) {
        LoyaltyRewardType.FreeProduct -> "Café gratis"
        LoyaltyRewardType.Merch -> "Merch"
        LoyaltyRewardType.CafeDelDia -> "Café del día"
        is LoyaltyRewardType.Other -> "Premio"
    }

    private fun redeem(id: String) {
        if (data.redeemingID != null) return
        data = data.copy(redeemingID = id, errorMessage = null)
        val key = idempotencyKeys.getOrPut(id) { UUID.randomUUID().toString() }
        scope.launch {
            runCatching { loyalty.redeem(id, key) }
                .onSuccess { outcome ->
                    idempotencyKeys.remove(id)
                    data = data.copy(redeemingID = null, resultMessage = confirmation(outcome))
                    Haptics.success()
                    bus.publish(BeansEarnedEvent(amount = -outcome.voucher.cost))
                    // Hay un vale nuevo: el Checkout recarga su lista y el Perfil coalescea
                    // este evento con el BeansEarnedEvent de arriba (una sola recarga).
                    bus.publish(RewardRedeemedEvent())
                    performLoad()
                }
                .onFailure { data = data.copy(redeemingID = null, errorMessage = it.message ?: "No se pudo canjear.") }
        }
    }

    private fun beginShipping(voucherID: String) {
        if (data.shippingID != null) return
        val voucher = data.vouchers.firstOrNull { it.id == voucherID } ?: return
        data = data.copy(
            shippingForm = profile?.load() ?: CheckoutForm(),
            shippingGrind = GrindOption.Default,
            shippingErrors = emptyMap(),
            shippingSubmitError = null,
            shippingVoucher = voucher,
        )
    }

    private fun cancelShipping() {
        if (data.shippingID != null) return
        data = data.copy(shippingVoucher = null, shippingErrors = emptyMap(), shippingSubmitError = null)
    }

    private fun confirmShipping() {
        val voucher = data.shippingVoucher ?: return
        if (data.shippingID != null) return
        val form = data.shippingForm.trimmed()
        val errors = CheckoutValidator.validate(form)
        if (errors.isNotEmpty()) {
            data = data.copy(shippingErrors = errors)
            return
        }
        data = data.copy(shippingErrors = emptyMap(), shippingSubmitError = null, shippingForm = form, shippingID = voucher.id)
        val productID = data.shipCoffeeByVoucher[voucher.id]
        val grind = if (voucher.isCoffee) data.shippingGrind.key else null
        scope.launch {
            runCatching { loyalty.redeemDirect(voucher.id, productID, grind, form) }
                .onSuccess {
                    profile?.save(form)
                    data = data.copy(
                        shippingID = null,
                        shippingVoucher = null,
                        resultMessage = "¡Listo! Tu premio sale con envío gratis en San Miguel. Te contactamos para coordinar la entrega.",
                    )
                    Haptics.success()
                    // Se consumió un vale: avisa al Checkout (vales) y al Perfil (estado).
                    bus.publish(RewardRedeemedEvent())
                    performLoad()
                }
                .onFailure { data = data.copy(shippingID = null, shippingSubmitError = it.message ?: "No se pudo pedir el envío.") }
        }
    }

    private fun confirmation(outcome: RedeemOutcome): String {
        val base = "¡Listo! Canjeaste ${outcome.voucher.label}."
        return when (outcome.voucher.type) {
            LoyaltyRewardType.CafeDelDia -> "$base Sumá el café del día a tu compra y el descuento se aplica solo."
            LoyaltyRewardType.FreeProduct, LoyaltyRewardType.Merch -> "$base Usalo en tu próxima compra o pedilo directo con envío gratis en San Miguel."
            is LoyaltyRewardType.Other -> base
        }
    }

    private fun grouped(value: Int): String {
        val sign = if (value < 0) "-" else ""
        val digits = abs(value).toString()
        val sb = StringBuilder()
        val n = digits.length
        for (i in 0 until n) {
            if (i > 0 && (n - i) % 3 == 0) sb.append('.')
            sb.append(digits[i])
        }
        return "$sign$sb"
    }

    private companion object {
        val dayMonth = SimpleDateFormat("d MMM", Locale("es", "AR"))
    }
}

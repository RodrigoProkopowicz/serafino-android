package com.serafino.feature.store

import com.serafino.architecture.AuthChangedEvent
import com.serafino.architecture.AuthProviding
import com.serafino.architecture.AuthUser
import com.serafino.architecture.BusEvent
import com.serafino.architecture.CartChangedEvent
import com.serafino.architecture.EventBus
import com.serafino.architecture.events
import com.serafino.domain.entities.loyalty.BeanTransaction
import com.serafino.domain.entities.loyalty.BeanTransactionKind
import com.serafino.domain.entities.loyalty.CafeDelDia
import com.serafino.domain.entities.loyalty.DirectRedeemOutcome
import com.serafino.domain.entities.loyalty.LoyaltyAccount
import com.serafino.domain.entities.loyalty.LoyaltyConfig
import com.serafino.domain.entities.loyalty.LoyaltyReward
import com.serafino.domain.entities.loyalty.LoyaltyRewardType
import com.serafino.domain.entities.loyalty.LoyaltyTier
import com.serafino.domain.entities.loyalty.LoyaltyVoucher
import com.serafino.domain.entities.loyalty.RedeemOutcome
import com.serafino.domain.entities.store.CartLine
import com.serafino.domain.entities.store.Ficha
import com.serafino.domain.entities.store.Product
import com.serafino.domain.entities.store.ProductFormat
import com.serafino.domain.entities.store.ProductReviews
import com.serafino.domain.entities.store.pricing
import com.serafino.domain.services.AccountService
import com.serafino.domain.services.CartStoring
import com.serafino.domain.services.CheckoutForm
import com.serafino.domain.services.CheckoutNotice
import com.serafino.domain.services.CheckoutPreference
import com.serafino.domain.services.CheckoutProfileStoring
import com.serafino.domain.services.CheckoutRequest
import com.serafino.domain.services.CheckoutService
import com.serafino.domain.services.LoyaltyProviding
import com.serafino.domain.services.ProductCatalogProviding
import com.serafino.domain.services.PromoResult
import com.serafino.domain.services.ReviewsProviding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.Calendar
import java.util.Date

class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}

fun testDate(y: Int, m: Int, d: Int): Date =
    Calendar.getInstance().apply { clear(); set(y, m - 1, d, 12, 0, 0) }.time

/** Datos de muestra de productos. Espeja `SampleProducts` de iOS. */
object SampleProducts {
    val aurora = Product(
        id = "aurora", name = "Aurora", image = "/img/products/aurora.jpg", origin = "Colombia",
        roast = "Media", weight = "250 g", price = 20000, promoActive = true, promoPrice = 17000,
        accent = listOf("e6c78f", "9c6b2f"), notes = listOf("Canela", "Caramelo suave"), badge = null,
        featured = false, hidden = false,
        formats = listOf(
            ProductFormat("Degustación", "125 g", 12000, 10200),
            ProductFormat("Estándar", "250 g", 20000, 17000),
            ProductFormat("Rendidor", "500 g", 37000, 31450),
        ),
        description = "Arábica de Colombia. Intensidad 4/10.",
        ficha = Ficha("Cúcuta", "1.200 msnm", "Arábica", "Lavado", "Cooperativas", "Oct – Dic", "Una historia de altura."),
        comboItems = null,
    )
    val premium = Product(
        id = "serafino-casa", name = "Serafino Premium", image = "/img/products/premium.jpg",
        origin = "Brasil + Colombia", roast = "Media-oscura", weight = "250 g", price = 19000,
        promoActive = true, promoPrice = 16150, accent = listOf("c79a5b", "5c3a24"), notes = listOf("Chocolate", "Caramelo"),
        badge = "Exclusivo", featured = true, hidden = false,
        formats = listOf(ProductFormat("Estándar", "250 g", 19000, 16150)),
        description = "Mezcla de la casa.", ficha = null, comboItems = null,
    )
    val plain = Product(
        id = "plain", name = "Plano", image = "/img/products/plain.jpg", origin = "Brasil", roast = "Media",
        weight = "250 g", price = 18000, promoActive = false, promoPrice = null, accent = listOf("9a6b45", "3a2316"),
        notes = listOf("Cacao"), badge = null, featured = false, hidden = false, formats = emptyList(),
        description = "Sin promo.", ficha = null, comboItems = null,
    )
    val notturno = Product(
        id = "notturno", name = "Notturno", image = "/img/products/notturno.jpg", origin = "Brasil + Colombia",
        roast = "Media-oscura", weight = "250 g", price = 20000, promoActive = false, promoPrice = null,
        accent = listOf("8a5230", "2a1610"), notes = listOf("Cacao", "Almendra"), badge = null, featured = false,
        hidden = false, formats = emptyList(), description = "Blend intenso.", ficha = null, comboItems = null,
    )
    val hidden = Product(
        id = "oculto", name = "Oculto", image = "/img/products/oculto.jpg", origin = "Brasil", roast = "Media",
        weight = "250 g", price = 15000, promoActive = false, promoPrice = null, accent = listOf("000000", "111111"),
        notes = emptyList(), badge = null, featured = false, hidden = true, formats = emptyList(),
        description = "Oculto.", ficha = null, comboItems = null,
    )
    val all: List<Product> = listOf(aurora, premium, plain, notturno, hidden)
}

/**
 * Línea de prueba a partir de un producto (formato Estándar). `discounted` espeja
 * producción por default: `unitPrice` ya viene con la promo aplicada, así que la
 * línea queda marcada igual que al agregar desde el detalle (`pricing.active`) y
 * el código promocional no se apila encima. Pasá `discounted` para forzar un caso.
 */
fun cartLine(product: Product, quantity: Int = 1, discounted: Boolean? = null): CartLine = CartLine(
    productID = product.id, name = product.name, image = product.image,
    formatLabel = "Estándar", formatWeight = product.weight,
    unitPrice = product.pricing.price, quantity = quantity,
    discounted = discounted ?: product.pricing.active,
)

class MockProductCatalog(
    var products: List<Product> = SampleProducts.all,
    var error: Throwable? = null,
) : ProductCatalogProviding {
    /**
     * Catálogo "conocido" previo (simula el snapshot en disco del provider real): permite
     * testear el arranque instantáneo desde caché + revalidación en segundo plano.
     */
    var seeded: List<Product> = emptyList()
    var seededDate: Date? = null

    /** Demora artificial de la "red" (para observar estados intermedios con reloj virtual). */
    var delayMillis: Long = 0
    var loadCount = 0; private set

    override suspend fun loadProducts(): List<Product> {
        loadCount++
        if (delayMillis > 0) delay(delayMillis)
        error?.let { throw it }
        return products.filter { !it.hidden }
    }
    override fun product(id: String): Product? = products.firstOrNull { it.id == id }
    override val cachedProducts: List<Product> get() = seeded
    override val cachedProductsDate: Date? get() = seededDate
}

/**
 * Colecciona los eventos de un tipo publicados en el bus, para asertar que un flujo
 * los emite (y cuántas veces). Crear ANTES de disparar el flujo (el bus no tiene replay).
 */
class EventProbe<E : BusEvent>(bus: EventBus, type: Class<E>) {
    val events = mutableListOf<E>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    init {
        scope.launch { bus.events(type).collect { events.add(it) } }
    }
}

inline fun <reified E : BusEvent> eventProbe(bus: EventBus): EventProbe<E> = EventProbe(bus, E::class.java)

class MockCartStore(private val bus: EventBus, lines: List<CartLine> = emptyList()) : CartStoring {
    override var lines: List<CartLine> = lines
        private set
    var clearCount = 0; private set
    override val count: Int get() = lines.sumOf { it.quantity }
    override val subtotal: Int get() = lines.sumOf { it.lineTotal }

    override fun add(line: CartLine) {
        val idx = lines.indexOfFirst { it.id == line.id }
        lines = if (idx >= 0) lines.toMutableList().also {
            // Espeja el tope de RoomCartStore (acumulación acotada a MAX_ITEM_QUANTITY).
            it[idx] = it[idx].copy(quantity = minOf(CartLine.MAX_ITEM_QUANTITY, it[idx].quantity + line.quantity))
        }
        else lines + line
        publish()
    }
    override fun setQuantity(id: String, quantity: Int) {
        val idx = lines.indexOfFirst { it.id == id }
        if (idx < 0) return
        lines = if (quantity <= 0) lines.filterNot { it.id == id }
        else lines.toMutableList().also { it[idx] = it[idx].copy(quantity = quantity) }
        publish()
    }
    override fun remove(id: String) { lines = lines.filterNot { it.id == id }; publish() }
    override fun clear() { clearCount++; lines = emptyList(); publish() }
    private fun publish() = bus.publish(CartChangedEvent(count))
}

class MockCheckoutService : CheckoutService {
    var preference: CheckoutPreference? = null
    var preferenceError: Throwable? = null
    var promoResult = PromoResult(valid = false, code = null, percent = null, error = "Inválido")
    var notice: CheckoutNotice? = null
    var orderQueue: MutableList<com.serafino.domain.entities.store.Order> = mutableListOf()
    val createdRequests = mutableListOf<CheckoutRequest>()
    var fetchCount = 0; private set

    override suspend fun createPreference(request: CheckoutRequest): CheckoutPreference {
        createdRequests.add(request)
        preferenceError?.let { throw it }
        return preference ?: CheckoutPreference("order-1", "https://www.mercadopago.com.ar/checkout", null)
    }
    override suspend fun fetchOrder(id: String): com.serafino.domain.entities.store.Order {
        fetchCount++
        if (orderQueue.size > 1) return orderQueue.removeAt(0)
        return orderQueue.firstOrNull() ?: throw RuntimeException("Respuesta inválida del servidor.")
    }
    override suspend fun validatePromo(code: String): PromoResult = promoResult
    override suspend fun checkoutNotice(): CheckoutNotice? = notice
}

class FakeCheckoutProfileStore(var stored: CheckoutForm? = null) : CheckoutProfileStoring {
    val savedForms = mutableListOf<CheckoutForm>()
    override fun load(): CheckoutForm? = stored
    override fun save(form: CheckoutForm) { stored = form; savedForms.add(form) }
}

class MockReviewsProvider(var result: ProductReviews = ProductReviews.empty, var error: Throwable? = null) : ReviewsProviding {
    override suspend fun reviews(productID: String): ProductReviews {
        error?.let { throw it }
        return result
    }
}

/** Escalera de fidelidad de muestra (inline; feature:store no depende de :data). */
object SampleLoyalty {
    val tiers = listOf(
        LoyaltyTier("base", "Base", "Acá empieza", 0, "Desde 0 granos", emptyList(), "leaf", "5b4231", 5),
        LoyaltyTier("intermedio", "Intermedio", "Seguí", 300, "Desde 300 granos", emptyList(), "cup", "a3692c", 10),
        LoyaltyTier("top", "Top", "La cima", 700, "Desde 700 granos", emptyList(), "star", "c0833f", 15),
    )

    fun rewards(unlockedTop: Boolean = false, affordableTop: Boolean = false): List<LoyaltyReward> = listOf(
        reward("free-125", LoyaltyRewardType.FreeProduct, 250, "base", "Café Degustación 125 g", "125 g", true, true),
        reward("free-250", LoyaltyRewardType.FreeProduct, 400, "intermedio", "Café Estándar 250 g", "250 g", true, true),
        reward("free-500", LoyaltyRewardType.FreeProduct, 700, "top", "Café Rendidor 500 g", "500 g", unlockedTop, affordableTop),
        reward("merch-tote", LoyaltyRewardType.Merch, 120, "base", "Tote Serafino", null, true, true),
        reward("merch-taza", LoyaltyRewardType.Merch, 180, "intermedio", "Taza Serafino", null, true, true),
    )

    val cafeDelDia = CafeDelDia("aurora", 60, "60 granos", "tue", "Martes", 10, 3000, true)

    fun account(points: Int = 450, lifetime: Int = 450): LoyaltyAccount = LoyaltyAccount.make(
        points = points, lifetimePoints = lifetime, amountPerPoint = 1000, tiers = tiers,
        rewards = rewards(), cafeDelDia = cafeDelDia,
        history = listOf(
            BeanTransaction("e1", "earn", "ord-1", "Aurora (250 g)", testDate(2026, 6, 18), 45, BeanTransactionKind.Purchase),
            BeanTransaction("e2", "redeem", null, null, testDate(2026, 6, 10), -250, BeanTransactionKind.Redeem),
            BeanTransaction("e3", "reversal", "ord-9", "Dolce (250 g)", testDate(2026, 6, 5), -20, BeanTransactionKind.Reversal),
        ),
    )

    fun voucher(
        id: String, type: LoyaltyRewardType = LoyaltyRewardType.FreeProduct, status: String = "active",
        format: String? = "250 g", productId: String? = null, pct: Int? = null, cap: Int? = null,
        // Lejos en el futuro: el filtro isUsable() descarta vencidos, así que el default debe estar vigente.
        expiresAt: Date? = testDate(2030, 1, 1),
    ): LoyaltyVoucher = LoyaltyVoucher(
        id = id, rewardId = "r-$id", type = type, cost = 400, label = "Vale $id",
        status = status, format = format, productId = productId, pct = pct, maxDiscountARS = cap,
        createdAt = testDate(2026, 6, 1), expiresAt = expiresAt,
    )

    private fun reward(id: String, type: LoyaltyRewardType, cost: Int, unlock: String, label: String, format: String?, unlocked: Boolean, affordable: Boolean) =
        LoyaltyReward(id, type, cost, "$cost granos", unlock, label, format, unlocked, affordable, unlocked && affordable)
}

class MockLoyaltyProvider(
    var account: LoyaltyAccount = SampleLoyalty.account(),
    var config: LoyaltyConfig? = null,
    var vouchersList: List<LoyaltyVoucher> = emptyList(),
) : LoyaltyProviding {
    var redeemResult: RedeemOutcome? = null
    var directResult = DirectRedeemOutcome(orderId = "order-free-1", voucher = null)
    var accountError: Throwable? = null
    var redeemError: Throwable? = null
    var directError: Throwable? = null
    val redeemCalls = mutableListOf<Pair<String, String?>>()
    val directCalls = mutableListOf<DirectCall>()
    var loadAccountCount = 0; private set
    var summaryCount = 0; private set
    var loadConfigCount = 0; private set
    var vouchersCount = 0; private set

    /** Demora artificial de las cargas (para testear el dedupe de cargas concurrentes). */
    var delayMillis: Long = 0

    data class DirectCall(val voucherId: String, val productId: String?, val grind: String?, val contact: CheckoutForm)

    override suspend fun loadPublicConfig(): LoyaltyConfig {
        loadConfigCount++
        return config ?: LoyaltyConfig(
            account.pointsName, account.amountPerPoint, SampleLoyalty.tiers, SampleLoyalty.rewards(), SampleLoyalty.cafeDelDia,
        )
    }
    override suspend fun loadAccount(): LoyaltyAccount {
        loadAccountCount++
        if (delayMillis > 0) delay(delayMillis)
        accountError?.let { throw it }
        return account
    }

    /**
     * Refresco liviano: cuenta aparte para asertar qué triggers usan la carga completa
     * y cuáles la resumida (la real pide solo `/me`).
     */
    override suspend fun loadAccountSummary(): LoyaltyAccount {
        summaryCount++
        if (delayMillis > 0) delay(delayMillis)
        accountError?.let { throw it }
        return account
    }
    override suspend fun redeem(rewardId: String, idempotencyKey: String?): RedeemOutcome {
        redeemCalls.add(rewardId to idempotencyKey)
        redeemError?.let { throw it }
        return redeemResult ?: RedeemOutcome(
            LoyaltyVoucher("v-$rewardId", rewardId, LoyaltyRewardType.FreeProduct, 250, "Café Degustación 125 g", "active", "125 g", null, null, null, null, null),
            balance = 200,
        )
    }
    override suspend fun redeemDirect(voucherId: String, productId: String?, grind: String?, contact: CheckoutForm): DirectRedeemOutcome {
        directCalls.add(DirectCall(voucherId, productId, grind, contact))
        directError?.let { throw it }
        return directResult
    }
    override suspend fun vouchers(): List<LoyaltyVoucher> {
        vouchersCount++
        return vouchersList
    }
}

class MockAuthService(
    private val bus: EventBus,
    override var currentUser: AuthUser? = null,
    var token: String? = "tok-abc",
) : AuthProviding {
    var signInError: Throwable? = null
    /** Si está seteado, `signOut` lanza SIN limpiar la sesión ni publicar el evento (espeja el fallo real). */
    var signOutError: Throwable? = null
    var signOutCount = 0; private set

    override suspend fun signIn() {
        signInError?.let { throw it }
        currentUser = AuthUser("u1", "Ana", "ana@mail.com", testDate(2025, 1, 1))
        bus.publish(AuthChangedEvent(isSignedIn = true))
    }
    override fun signOut() {
        signOutCount++
        signOutError?.let { throw it }
        currentUser = null
        bus.publish(AuthChangedEvent(isSignedIn = false))
    }
    override suspend fun idToken(): String? = token
}

class MockAccountService(var error: Throwable? = null) : AccountService {
    var deleteCount = 0; private set
    /** Demora artificial del borrado (para observar el estado `deleting` con reloj virtual). */
    var delayMillis: Long = 0

    override suspend fun deleteAccount() {
        deleteCount++
        if (delayMillis > 0) delay(delayMillis)
        error?.let { throw it }
    }
}

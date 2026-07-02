package com.serafino.feature.store.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.serafino.architecture.AnalyticsEvent
import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.CartChangedEvent
import com.serafino.architecture.EventBus
import com.serafino.architecture.Interactor
import com.serafino.architecture.NoOpAnalytics
import com.serafino.architecture.events
import com.serafino.domain.BackendConfig
import com.serafino.domain.entities.store.CartLine
import com.serafino.domain.entities.store.Ficha
import com.serafino.domain.entities.store.Pricing
import com.serafino.domain.entities.store.Product
import com.serafino.domain.entities.store.ProductReview
import com.serafino.domain.entities.store.hasFicha
import com.serafino.domain.entities.store.intensity
import com.serafino.domain.entities.store.isCombo
import com.serafino.domain.entities.store.pricing
import com.serafino.domain.services.CartStoring
import com.serafino.domain.services.ProductCatalogProviding
import com.serafino.domain.services.ReviewsProviding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Arma el modelo de presentación de un café: formatos con precio efectivo, ficha, reseñas y
 * acciones (agregar al carrito). Espeja `ProductDetailInteractor` de iOS.
 */
class ProductDetailInteractor(
    private val productID: String,
    private val catalog: ProductCatalogProviding,
    private val cart: CartStoring,
    private val reviewsProvider: ReviewsProviding,
    private val bus: EventBus,
    private val analytics: AnalyticsTracking = NoOpAnalytics(),
    /** Ventana de frescura del catálogo al reingresar al detalle (vuelta de un push). */
    private val staleTTL: Duration = 15.seconds,
) : Interactor<ProductDetailInteractor.Data, ProductDetailInteractor.Input, ProductDetailInteractor.State> {

    data class FormatOption(val label: String, val weight: String, val pricing: Pricing) {
        val id: String get() = weight
    }

    enum class ReviewsState { Idle, Loading, Loaded, Failed }

    data class Data(
        val name: String = "",
        val origin: String = "",
        val roast: String = "",
        val description: String = "",
        val imageURL: String? = null,
        val accent: List<String> = emptyList(),
        val badge: String? = null,
        val notes: List<String> = emptyList(),
        val intensity: Double? = null,
        val isCombo: Boolean = false,
        val formats: List<FormatOption> = emptyList(),
        val selectedWeight: String = "",
        val selectedPricing: Pricing = Pricing.zero,
        val quantity: Int = 1,
        val ficha: Ficha? = null,
        val cartCount: Int = 0,
        val reviews: List<ProductReview> = emptyList(),
        val reviewCount: Int = 0,
        val averageRating: Double? = null,
        val reviewsState: ReviewsState = ReviewsState.Idle,
        /** Se incrementa en cada "agregar al carrito" para disparar el toast de confirmación. */
        val addedTick: Int = 0,
    )

    sealed interface Input {
        data object OnAppear : Input
        data class SelectFormat(val weight: String) : Input
        data object IncrementQuantity : Input
        data object DecrementQuantity : Input
        data object AddToCart : Input
        data object LoadReviews : Input
    }

    enum class State { Idle, Loaded, NotFound }

    override var data by mutableStateOf(Data())
        private set
    override var state by mutableStateOf(State.Idle)
        private set

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var product: Product? = null

    init {
        data = data.copy(cartCount = cart.count)
        bindEvents()
    }

    override fun handle(input: Input) {
        when (input) {
            is Input.OnAppear -> load()
            is Input.SelectFormat -> selectFormat(input.weight)
            is Input.IncrementQuantity ->
                data = data.copy(quantity = min(CartLine.MAX_ITEM_QUANTITY, data.quantity + 1))
            is Input.DecrementQuantity -> data = data.copy(quantity = max(1, data.quantity - 1))
            is Input.AddToCart -> addToCart()
            is Input.LoadReviews -> loadReviews()
        }
    }

    fun dispose() = scope.cancel()

    private fun load() {
        // Reingreso al detalle (vuelta de un push): refrescamos precio/promo en segundo plano
        // sin pisar la selección de formato/cantidad del usuario.
        if (state == State.Loaded) {
            scope.launch { refreshPricing() }
            return
        }
        val cached = catalog.product(productID)
        if (cached != null) {
            apply(cached)
            // La caché puede venir del snapshot en disco (días de antigüedad) o estar
            // vencida: revalidamos precio/promo en segundo plano. Dentro del TTL es
            // gratis (loadProductsIfStale devuelve la caché sin red).
            scope.launch { refreshPricing() }
            return
        }
        scope.launch {
            runCatching { catalog.loadProducts() }
                .onSuccess {
                    val product = catalog.product(productID)
                    if (product != null) apply(product) else state = State.NotFound
                }
                .onFailure { state = State.NotFound }
        }
    }

    private fun formatOptions(product: Product): List<FormatOption> =
        product.formats.map { format ->
            // Una sola regla de precio: honra la promo del formato si la trae,
            // si no hereda la del producto (ver `pricing(weight)`).
            FormatOption(format.label, format.weight, product.pricing(format.weight))
        }.ifEmpty {
            listOf(FormatOption("Estándar", product.weight, product.pricing))
        }

    /**
     * Revalida el catálogo (gateado por TTL) y re-aplica SOLO lo que afecta al precio: los
     * formatos con su promo y el precio del formato elegido. El formato y la cantidad que
     * eligió el usuario quedan como están; si su formato dejó de existir, cae al estándar.
     */
    private suspend fun refreshPricing() {
        runCatching { catalog.loadProductsIfStale(staleTTL) }.getOrNull() ?: return
        val updated = catalog.product(productID) ?: return
        product = updated
        val options = formatOptions(updated)
        val current = options.firstOrNull { it.weight == data.selectedWeight }
        if (current != null) {
            data = data.copy(badge = updated.badge, formats = options, selectedPricing = current.pricing)
        } else {
            val standard = options.firstOrNull { it.weight == updated.weight } ?: options.first()
            data = data.copy(
                badge = updated.badge,
                formats = options,
                selectedWeight = standard.weight,
                selectedPricing = standard.pricing,
            )
        }
    }

    private fun apply(product: Product) {
        this.product = product
        val options = formatOptions(product)
        val standard = options.firstOrNull { it.weight == product.weight } ?: options.first()

        data = data.copy(
            name = product.name,
            origin = product.origin,
            roast = product.roast,
            description = product.description,
            imageURL = BackendConfig.imageURL(product.image),
            accent = product.accent,
            badge = product.badge,
            notes = product.notes,
            intensity = product.intensity,
            isCombo = product.isCombo,
            ficha = if (product.hasFicha) product.ficha else null,
            formats = options,
            selectedWeight = standard.weight,
            selectedPricing = standard.pricing,
        )
        state = State.Loaded
        analytics.track(AnalyticsEvent.ViewItem(product.id, product.name, data.selectedPricing.price))
        loadReviews()
    }

    private fun selectFormat(weight: String) {
        val option = data.formats.firstOrNull { it.weight == weight } ?: return
        data = data.copy(selectedWeight = option.weight, selectedPricing = option.pricing)
    }

    private fun addToCart() {
        val product = product ?: return
        val option = data.formats.firstOrNull { it.weight == data.selectedWeight } ?: return
        cart.add(
            CartLine(
                productID = product.id,
                name = product.name,
                image = product.image,
                formatLabel = option.label,
                formatWeight = option.weight,
                unitPrice = option.pricing.price,
                quantity = data.quantity,
                // Un único descuento por producto: si el formato ya trae promo (o es combo), el
                // código promocional no se apila encima en el checkout (espeja resolveOrderItems).
                discounted = option.pricing.active || product.isCombo,
            ),
        )
        analytics.track(AnalyticsEvent.AddToCart(product.id, product.name, option.pricing.price, data.quantity))
        data = data.copy(quantity = 1, addedTick = data.addedTick + 1)
    }

    private fun loadReviews() {
        data = data.copy(reviewsState = ReviewsState.Loading)
        scope.launch {
            runCatching { reviewsProvider.reviews(productID) }
                .onSuccess { result ->
                    data = data.copy(
                        reviews = result.reviews,
                        reviewCount = result.count,
                        averageRating = result.average,
                        reviewsState = ReviewsState.Loaded,
                    )
                }
                .onFailure { data = data.copy(reviewsState = ReviewsState.Failed) }
        }
    }

    private fun bindEvents() {
        scope.launch { bus.events<CartChangedEvent>().collect { data = data.copy(cartCount = it.count) } }
    }
}

package com.serafino.feature.store.catalog

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
import com.serafino.designsystem.store.ProductCardModel
import com.serafino.domain.BackendConfig
import com.serafino.domain.entities.store.Product
import com.serafino.domain.entities.store.RoastFilter
import com.serafino.domain.entities.store.pricing
import com.serafino.domain.services.CartStoring
import com.serafino.domain.services.ProductCatalogProviding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Carga el catálogo de la tienda desde Firestore, arma las tarjetas con el precio efectivo y
 * mantiene el contador de la bolsa en sync vía EventBus. Espeja `StoreCatalogInteractor` de iOS.
 */
class StoreCatalogInteractor(
    private val catalog: ProductCatalogProviding,
    private val cart: CartStoring,
    private val bus: EventBus,
    private val analytics: AnalyticsTracking = NoOpAnalytics(),
) : Interactor<StoreCatalogInteractor.Data, StoreCatalogInteractor.Input, StoreCatalogInteractor.State> {

    data class Data(
        val featured: ProductCardModel? = null,
        val products: List<ProductCardModel> = emptyList(),
        val cartCount: Int = 0,
        val roastFilters: List<RoastFilter> = emptyList(),
        val selectedRoast: RoastFilter = RoastFilter.All,
    )

    sealed interface Input {
        data object OnAppear : Input
        data object Retry : Input
        data class SelectRoast(val roast: RoastFilter) : Input
    }

    sealed interface State {
        data object Idle : State
        data object Loading : State
        data object Loaded : State
        data object Empty : State
        data class Error(val message: String) : State
    }

    override var data by mutableStateOf(Data())
        private set
    override var state by mutableStateOf<State>(State.Idle)
        private set

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var didLoad = false
    private var restProducts: List<Product> = emptyList()

    init {
        data = data.copy(cartCount = cart.count)
        bindEvents()
    }

    override fun handle(input: Input) {
        when (input) {
            is Input.OnAppear -> if (!didLoad) load()
            is Input.Retry -> load()
            is Input.SelectRoast -> {
                if (input.roast != data.selectedRoast) {
                    data = data.copy(selectedRoast = input.roast)
                    applyRoastFilter()
                }
            }
        }
    }

    fun dispose() = scope.cancel()

    private fun load() {
        didLoad = true
        state = State.Loading
        analytics.track(AnalyticsEvent.Screen("store_catalog"))
        scope.launch {
            runCatching { catalog.loadProducts() }
                .onSuccess { build(it) }
                .onFailure { error ->
                    if (data.products.isEmpty() && data.featured == null) {
                        state = State.Error(error.message ?: "No se pudo cargar el catálogo.")
                    }
                }
        }
    }

    private fun build(products: List<Product>) {
        val featured = products.firstOrNull { it.featured }
        restProducts = products.filter { it.id != featured?.id }.sortedBy { it.name }
        val buckets = restProducts.mapNotNull { RoastFilter.bucket(it.roast) }.toSet()
        val filters = RoastFilter.entries.filter { it == RoastFilter.All || buckets.contains(it) }
        var selected = data.selectedRoast
        if (!filters.contains(selected)) selected = RoastFilter.All
        data = data.copy(
            featured = featured?.let { card(it) },
            roastFilters = filters,
            selectedRoast = selected,
        )
        applyRoastFilter()
    }

    private fun applyRoastFilter() {
        val filtered = if (data.selectedRoast == RoastFilter.All) restProducts
        else restProducts.filter { RoastFilter.bucket(it.roast) == data.selectedRoast }
        data = data.copy(products = filtered.map { card(it) })
        state = if (data.featured == null && restProducts.isEmpty()) State.Empty else State.Loaded
    }

    private fun card(product: Product): ProductCardModel = ProductCardModel(
        id = product.id,
        name = product.name,
        origin = product.origin,
        roast = product.roast,
        imageURL = BackendConfig.imageURL(product.image),
        accent = product.accent,
        pricing = product.pricing,
        badge = product.badge,
        notes = product.notes,
    )

    private fun bindEvents() {
        scope.launch {
            bus.events<CartChangedEvent>().collect { data = data.copy(cartCount = it.count) }
        }
    }
}

package com.serafino.feature.store.catalog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.serafino.architecture.AnalyticsEvent
import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.AppDidBecomeActiveEvent
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
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Carga el catálogo de la tienda desde Firestore, arma las tarjetas con el precio efectivo y
 * mantiene el contador de la bolsa en sync vía EventBus. Espeja `StoreCatalogInteractor` de iOS.
 *
 * Frescura: la primera carga muestra el último catálogo conocido (snapshot) al instante
 * y revalida por red; los reingresos al tab y la vuelta al foreground refrescan en
 * segundo plano solo si el TTL venció; el pull-to-refresh y el tick periódico de
 * [autoRefreshInterval] (mientras la Tienda está visible) fuerzan la recarga.
 */
class StoreCatalogInteractor(
    private val catalog: ProductCatalogProviding,
    private val cart: CartStoring,
    private val bus: EventBus,
    private val analytics: AnalyticsTracking = NoOpAnalytics(),
    /** Cada cuánto se auto-refresca el catálogo mientras la Tienda está visible. */
    private val autoRefreshInterval: Duration = 5.seconds,
    /** Ventana de frescura: reingresos al tab / foreground dentro de este TTL no tocan la red. */
    private val staleTTL: Duration = 15.seconds,
    private val now: () -> Date = ::Date,
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

    /**
     * Última carga POR RED exitosa: gatea los refrescos de ciclo de vida (TTL). Arranca
     * con la fecha del snapshot si se mostró desde caché (siempre vencida → revalida).
     */
    var lastUpdatedAt: Date? = null
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
            is Input.OnAppear -> {
                // Reingreso al tab con catálogo ya cargado: refrescamos en segundo plano (sin
                // parpadeo) solo si el TTL venció — alternar tabs rápido no cuesta red. Si la
                // carga inicial sigue en curso, no duplicamos el pedido.
                if (didLoad) {
                    if (state == State.Loading) return
                    refreshIfStale()
                } else {
                    load()
                }
            }
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

    /**
     * Mientras la Tienda esté en pantalla, refresca el catálogo en segundo plano (sin parpadeo)
     * cada [autoRefreshInterval]. El `LaunchedEffect` de la pantalla cancela este bucle al salir
     * del tab, así no consultamos al backend desde otras pestañas.
     */
    suspend fun autoRefresh() {
        while (currentCoroutineContext().isActive) {
            delay(autoRefreshInterval)
            performLoad()
        }
    }

    /**
     * Pull-to-refresh: fuerza la recarga (ignora el TTL) sin parpadeo; el control nativo
     * ya muestra su spinner.
     */
    suspend fun refresh() {
        performLoad()
    }

    // MARK: - Private

    private val isStale: Boolean
        get() {
            val last = lastUpdatedAt ?: return true
            return now().time - last.time > staleTTL.inWholeMilliseconds
        }

    /** Refresco en segundo plano gateado por TTL (reingreso al tab / vuelta al foreground). */
    private fun refreshIfStale() {
        if (!isStale) return
        scope.launch { performLoad() }
    }

    private fun load() {
        didLoad = true
        analytics.track(AnalyticsEvent.Screen("store_catalog"))
        // Arranque con caché (snapshot en disco u otra pantalla que ya cargó): mostramos el
        // último catálogo conocido al instante, sin skeleton, y revalidamos por red detrás.
        val cached = catalog.cachedProducts
        if (cached.isNotEmpty()) {
            build(cached)
            lastUpdatedAt = catalog.cachedProductsDate
            scope.launch { performLoad() }
            return
        }
        state = State.Loading
        scope.launch { performLoad() }
    }

    private suspend fun performLoad() {
        runCatching { catalog.loadProducts() }
            .onSuccess { products ->
                build(products)
                lastUpdatedAt = now()
            }
            .onFailure { error ->
                // Si ya hay contenido en pantalla, no lo reemplazamos por un error: queda el
                // catálogo anterior (lastUpdatedAt no avanza, así el próximo trigger reintenta).
                if (data.products.isEmpty() && data.featured == null) {
                    state = State.Error(error.message ?: "No se pudo cargar el catálogo.")
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
        // Vuelta al foreground (un solo publisher a nivel App): refresco gateado por TTL,
        // así un background corto no cuesta red y uno largo revalida el catálogo.
        scope.launch {
            bus.events<AppDidBecomeActiveEvent>().collect {
                if (didLoad && state != State.Loading) refreshIfStale()
            }
        }
    }
}

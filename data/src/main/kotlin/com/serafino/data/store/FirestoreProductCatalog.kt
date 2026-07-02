package com.serafino.data.store

import com.serafino.domain.BackendConfig
import com.serafino.domain.entities.store.Product
import com.serafino.domain.services.ProductCatalogProviding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Date
import kotlin.time.Duration

/**
 * Gateway del catálogo: lee Firestore por REST (lectura pública, sin SDK) y cachea
 * el último catálogo para que el detalle resuelva por id sin volver a la red. El
 * parsing del formato de "valores tipados" vive en [FirestoreCatalogMapper] (puro,
 * testeable); acá queda el transporte + la caché + la frescura:
 *  • dedupe en vuelo: pedidos concurrentes comparten UNA sola request;
 *  • TTL ([loadProductsIfStale]): dentro de la ventana devuelve la caché sin red;
 *  • snapshot en disco (bytes crudos de la última respuesta, re-parseados con el
 *    mapper): el arranque sin red muestra el último catálogo conocido.
 * Espeja `FirestoreProductCatalog` de iOS.
 */
class FirestoreProductCatalog(
    private val client: OkHttpClient = Http.shared,
    private val snapshotFile: File? = null,
) : ProductCatalogProviding {
    @Volatile
    private var cache: List<Product> = emptyList()

    /**
     * Fecha del contenido de [cache]: la última carga por red, o la fecha del snapshot
     * si todavía no se revalidó en esta sesión.
     */
    @Volatile
    private var cacheDate: Date? = null

    /** Última carga POR RED exitosa (el snapshot no cuenta: siempre se revalida). */
    @Volatile
    private var lastLoadedAt: Date? = null

    /**
     * El fetch corre en un scope propio (no en el del interactor que lo pidió): si el dueño
     * original se va de la pantalla, los demás que comparten la request igual reciben el
     * resultado — el análogo del `Task` no estructurado de iOS.
     */
    private val fetchScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val inFlightLock = Any()
    private var inFlight: Deferred<List<Product>>? = null

    init {
        seedFromSnapshot()
    }

    override suspend fun loadProducts(): List<Product> {
        // Pedidos concurrentes (tienda + detalle + canje + checkout) comparten una request.
        val task = synchronized(inFlightLock) {
            inFlight ?: fetchScope.async { fetchProducts() }.also { deferred ->
                inFlight = deferred
                deferred.invokeOnCompletion {
                    synchronized(inFlightLock) { if (inFlight === deferred) inFlight = null }
                }
            }
        }
        return task.await()
    }

    override suspend fun loadProductsIfStale(maxAge: Duration): List<Product> {
        val loadedAt = lastLoadedAt
        if (loadedAt != null && cache.isNotEmpty() &&
            Date().time - loadedAt.time <= maxAge.inWholeMilliseconds
        ) {
            return cache
        }
        return loadProducts()
    }

    override fun product(id: String): Product? = cache.firstOrNull { it.id == id }

    override val cachedProducts: List<Product> get() = cache

    override val cachedProductsDate: Date? get() = cacheDate

    // MARK: - Transporte

    private suspend fun fetchProducts(): List<Product> {
        // `runQuery` con filtro `hidden == false` (POST): un list sin filtro lo deniegan las
        // reglas de Firestore (solo los productos visibles son legibles públicamente).
        val body = BackendConfig.productsQueryBody.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(BackendConfig.productsQueryURL)
            .post(body)
            .build()
        val (status, responseBody) = Http.execute(request, client)
        if (status !in 200..299) throw ApiException.CatalogUnavailable
        val products = FirestoreCatalogMapper.products(responseBody)
        cache = products
        lastLoadedAt = Date()
        cacheDate = lastLoadedAt
        saveSnapshot(responseBody)
        return products
    }

    // MARK: - Snapshot en disco

    /** Bytes crudos de la última respuesta buena: mismo formato que la red, mismo mapper. */
    private fun saveSnapshot(body: String) {
        val file = snapshotFile ?: return
        runCatching {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(body)
            if (!tmp.renameTo(file)) {
                file.writeText(body)
                tmp.delete()
            }
        }
    }

    private fun seedFromSnapshot() {
        val file = snapshotFile ?: return
        val products = runCatching {
            if (!file.exists()) return
            FirestoreCatalogMapper.products(file.readText())
        }.getOrNull() ?: return
        if (products.isEmpty()) return
        cache = products
        // La fecha del archivo es la de la última respuesta guardada; NO seteamos
        // `lastLoadedAt`: el snapshot siempre está vencido y se revalida por red.
        cacheDate = Date(file.lastModified())
    }
}

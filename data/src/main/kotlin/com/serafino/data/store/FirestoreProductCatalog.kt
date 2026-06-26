package com.serafino.data.store

import com.serafino.domain.BackendConfig
import com.serafino.domain.entities.store.Product
import com.serafino.domain.services.ProductCatalogProviding
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Gateway del catálogo: lee Firestore por REST (lectura pública, sin SDK) y cachea el último
 * catálogo para que el detalle resuelva por id sin volver a la red. Espeja
 * `FirestoreProductCatalog` de iOS.
 */
class FirestoreProductCatalog(
    private val client: OkHttpClient = Http.shared,
) : ProductCatalogProviding {
    @Volatile
    private var cache: List<Product> = emptyList()

    override suspend fun loadProducts(): List<Product> {
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
        return products
    }

    override fun product(id: String): Product? = cache.firstOrNull { it.id == id }
}

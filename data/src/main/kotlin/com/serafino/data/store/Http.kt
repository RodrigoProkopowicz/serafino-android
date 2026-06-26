package com.serafino.data.store

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Cliente HTTP compartido del módulo de datos. */
internal object Http {
    val shared: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Ejecuta el request en IO y devuelve `(statusCode, body)`. El `client` es inyectable (default
     * el compartido) para poder stubbear el transporte en tests — el análogo de inyectar una
     * `URLSession` con `MockURLProtocol` en iOS.
     */
    suspend fun execute(request: Request, client: OkHttpClient = shared): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { resp ->
                resp.code to (resp.body?.string() ?: "")
            }
        }
}

package com.serafino.data.store

import com.serafino.domain.entities.store.ComboItem
import com.serafino.domain.entities.store.Ficha
import com.serafino.domain.entities.store.Product
import com.serafino.domain.entities.store.ProductFormat
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Decodifica el formato de "valores tipados" de Firestore (REST) a entidades `Product`. Pieza
 * PURA y sin estado: solo transforma JSON en modelos. Espeja `FirestoreCatalogMapper` de iOS.
 */
object FirestoreCatalogMapper {

    /**
     * Decodifica la respuesta del catálogo en productos visibles (descarta los `hidden`). Acepta
     * `runQuery` (array de filas `[{document:…}]`) o `documents.list` (`{documents:[…]}`).
     */
    fun products(body: String): List<Product> {
        val root = runCatching { JSONTokener(body).nextValue() }.getOrNull()
        val documents: List<JSONObject> = when (root) {
            is JSONArray -> (0 until root.length()).mapNotNull { i ->
                root.optJSONObject(i)?.optJSONObject("document")
            }
            is JSONObject -> root.optJSONArray("documents")?.let { arr ->
                (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
            } ?: emptyList()
            // Raíz ilegible (ni array ni objeto): respuesta inválida — fail-closed, como iOS.
            else -> throw ApiException.InvalidResponse
        }
        return documents.mapNotNull { product(it) }.filter { !it.hidden }
    }

    fun product(document: JSONObject): Product? {
        val name = document.optString("name", "")
        val id = name.substringAfterLast('/')
        if (id.isEmpty()) return null
        val fields = document.optJSONObject("fields") ?: return null

        return Product(
            id = id,
            name = fsString(fields, "name") ?: id,
            image = fsString(fields, "image") ?: "",
            origin = fsString(fields, "origin") ?: "",
            roast = fsString(fields, "roast") ?: "",
            weight = fsString(fields, "weight") ?: "",
            price = fsInt(fields, "price") ?: 0,
            promoActive = fsBool(fields, "promoActive") ?: false,
            promoPrice = fsInt(fields, "promoPrice"),
            accent = fsArray(fields, "accent").mapNotNull { fsStringValue(it) },
            notes = fsArray(fields, "notes").mapNotNull { fsStringValue(it) },
            badge = fsString(fields, "badge"),
            featured = fsBool(fields, "featured") ?: false,
            hidden = fsBool(fields, "hidden") ?: false,
            formats = fsArray(fields, "formats").mapNotNull { format(it) },
            description = fsString(fields, "description") ?: "",
            ficha = fsMap(fields, "ficha")?.let { ficha(it) },
            comboItems = if (fields.has("comboItems")) {
                fsArray(fields, "comboItems").mapNotNull { comboItem(it) }
            } else null,
        )
    }

    private fun format(value: JSONObject): ProductFormat? {
        val fields = mapFields(value) ?: return null
        val weight = fsString(fields, "weight") ?: ""
        if (weight.isEmpty()) return null
        return ProductFormat(
            label = fsString(fields, "label") ?: "",
            weight = weight,
            price = fsInt(fields, "price") ?: 0,
            promoPrice = fsInt(fields, "promoPrice"),
            // Ausente → null → hereda la promo del producto (compat total con datos existentes).
            promoActive = fsBool(fields, "promoActive"),
        )
    }

    private fun ficha(fields: JSONObject): Ficha = Ficha(
        region = fsString(fields, "region"),
        altitud = fsString(fields, "altitud"),
        variedad = fsString(fields, "variedad"),
        proceso = fsString(fields, "proceso"),
        productor = fsString(fields, "productor"),
        cosecha = fsString(fields, "cosecha"),
        historia = fsString(fields, "historia"),
    )

    private fun comboItem(value: JSONObject): ComboItem? {
        val fields = mapFields(value) ?: return null
        val productId = fsString(fields, "productId") ?: return null
        return ComboItem(
            productId = productId,
            name = fsString(fields, "name") ?: "",
            format = fsString(fields, "format") ?: "",
            qty = fsInt(fields, "qty") ?: 1,
        )
    }

    // MARK: - Lectores de "valores tipados" de Firestore

    private fun fsString(fields: JSONObject, key: String): String? =
        fields.optJSONObject(key)?.let { if (it.has("stringValue")) it.optString("stringValue") else null }

    private fun fsStringValue(value: JSONObject): String? =
        if (value.has("stringValue")) value.optString("stringValue") else null

    private fun fsInt(fields: JSONObject, key: String): Int? {
        val map = fields.optJSONObject(key) ?: return null
        map.optString("integerValue", "").toIntOrNull()?.let { return it }
        if (map.has("doubleValue")) return map.optDouble("doubleValue").toInt()
        return null
    }

    private fun fsBool(fields: JSONObject, key: String): Boolean? {
        val map = fields.optJSONObject(key) ?: return null
        return if (map.has("booleanValue")) map.optBoolean("booleanValue") else null
    }

    private fun fsArray(fields: JSONObject, key: String): List<JSONObject> {
        val arr = fields.optJSONObject(key)?.optJSONObject("arrayValue")?.optJSONArray("values")
            ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }

    private fun fsMap(fields: JSONObject, key: String): JSONObject? =
        fields.optJSONObject(key)?.optJSONObject("mapValue")?.optJSONObject("fields")

    private fun mapFields(value: JSONObject): JSONObject? =
        value.optJSONObject("mapValue")?.optJSONObject("fields")
}

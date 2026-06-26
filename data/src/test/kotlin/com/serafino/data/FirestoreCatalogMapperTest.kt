package com.serafino.data

import com.serafino.data.store.ApiException
import com.serafino.data.store.FirestoreCatalogMapper
import com.serafino.domain.entities.store.hasFicha
import com.serafino.domain.entities.store.intensity
import com.serafino.domain.entities.store.isCombo
import com.serafino.domain.entities.store.pricing
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Paridad con `FirestoreCatalogMapperTests` de iOS (decoding de "valores tipados" de Firestore). */
class FirestoreCatalogMapperTest {

    private val catalogJson = """
    {
      "documents": [
        {
          "name": "projects/x/databases/(default)/documents/products/aurora",
          "fields": {
            "name": {"stringValue": "Aurora"},
            "image": {"stringValue": "/img/products/aurora.jpg"},
            "origin": {"stringValue": "Colombia"},
            "roast": {"stringValue": "Media"},
            "weight": {"stringValue": "250 g"},
            "price": {"integerValue": "20000"},
            "promoActive": {"booleanValue": true},
            "promoPrice": {"integerValue": "17000"},
            "accent": {"arrayValue": {"values": [{"stringValue": "#e6c78f"}, {"stringValue": "#9c6b2f"}]}},
            "notes": {"arrayValue": {"values": [{"stringValue": "Canela"}, {"stringValue": "Caramelo"}]}},
            "badge": {"stringValue": "Nuevo"},
            "featured": {"booleanValue": true},
            "hidden": {"booleanValue": false},
            "formats": {"arrayValue": {"values": [
              {"mapValue": {"fields": {"label": {"stringValue": "Degustación"}, "weight": {"stringValue": "125 g"}, "price": {"integerValue": "12000"}, "promoPrice": {"integerValue": "10200"}}}},
              {"mapValue": {"fields": {"label": {"stringValue": "Estándar"}, "weight": {"stringValue": "250 g"}, "price": {"integerValue": "20000"}, "promoPrice": {"integerValue": "17000"}}}}
            ]}},
            "description": {"stringValue": "Arábica de Colombia. Intensidad 4/10."},
            "ficha": {"mapValue": {"fields": {"region": {"stringValue": "Cúcuta"}, "altitud": {"stringValue": "1.200 msnm"}, "proceso": {"stringValue": "Lavado"}}}}
          }
        },
        {
          "name": "projects/x/databases/(default)/documents/products/plain",
          "fields": {
            "name": {"stringValue": "Plano"}, "image": {"stringValue": "/img/products/plain.jpg"},
            "origin": {"stringValue": "Brasil"}, "roast": {"stringValue": "Media"}, "weight": {"stringValue": "250 g"},
            "price": {"integerValue": "18000"}, "promoActive": {"booleanValue": false},
            "description": {"stringValue": "Sin promo."}
          }
        },
        {
          "name": "projects/x/databases/(default)/documents/products/combo-finde",
          "fields": {
            "name": {"stringValue": "Combo Finde"}, "image": {"stringValue": "/img/products/combo.jpg"},
            "origin": {"stringValue": ""}, "roast": {"stringValue": ""}, "weight": {"stringValue": "500 g"},
            "price": {"doubleValue": 35000}, "promoActive": {"booleanValue": false},
            "description": {"stringValue": "Dos cafés con descuento."},
            "comboItems": {"arrayValue": {"values": [
              {"mapValue": {"fields": {"productId": {"stringValue": "aurora"}, "name": {"stringValue": "Aurora"}, "format": {"stringValue": "250 g"}, "qty": {"integerValue": "1"}}}},
              {"mapValue": {"fields": {"productId": {"stringValue": "plain"}, "name": {"stringValue": "Plano"}, "format": {"stringValue": "250 g"}, "qty": {"integerValue": "2"}}}}
            ]}}
          }
        },
        {
          "name": "projects/x/databases/(default)/documents/products/oculto",
          "fields": {"name": {"stringValue": "Oculto"}, "price": {"integerValue": "15000"}, "hidden": {"booleanValue": true}}
        },
        { "name": "projects/x/databases/(default)/documents/products/sinfields" }
      ]
    }
    """.trimIndent()

    private fun decode() = FirestoreCatalogMapper.products(catalogJson)

    @Test fun filtersHiddenAndMalformed() {
        val ids = decode().map { it.id }
        assertEquals(3, ids.size)
        assertTrue(ids.contains("aurora"))
        assertTrue(ids.contains("plain"))
        assertTrue(ids.contains("combo-finde"))
        assertFalse(ids.contains("oculto"))
        assertFalse(ids.contains("sinfields"))
    }

    @Test fun mapsScalarsArraysAndId() {
        val aurora = decode().first { it.id == "aurora" }
        assertEquals("Aurora", aurora.name)
        assertEquals("Colombia", aurora.origin)
        assertEquals("Media", aurora.roast)
        assertEquals(20000, aurora.price)
        assertTrue(aurora.promoActive)
        assertEquals(17000, aurora.promoPrice)
        assertEquals(listOf("#e6c78f", "#9c6b2f"), aurora.accent)
        assertEquals(listOf("Canela", "Caramelo"), aurora.notes)
        assertEquals("Nuevo", aurora.badge)
        assertTrue(aurora.featured)
        assertEquals(17000, aurora.pricing.price)
        assertEquals(4.0, aurora.intensity!!, 0.0001)
    }

    @Test fun mapsNestedFormats() {
        val aurora = decode().first { it.id == "aurora" }
        assertEquals(2, aurora.formats.size)
        val estandar = aurora.formats.first { it.weight == "250 g" }
        assertEquals("Estándar", estandar.label)
        assertEquals(20000, estandar.price)
        assertEquals(17000, estandar.promoPrice)
        assertEquals(10200, aurora.pricing("125 g").price)
    }

    @Test fun mapsFicha() {
        val aurora = decode().first { it.id == "aurora" }
        assertTrue(aurora.hasFicha)
        assertEquals("Cúcuta", aurora.ficha?.region)
        assertEquals("Lavado", aurora.ficha?.proceso)
        assertNull(aurora.ficha?.variedad)
    }

    @Test fun plainHasNoPromoNorFicha() {
        val plain = decode().first { it.id == "plain" }
        assertFalse(plain.promoActive)
        assertNull(plain.promoPrice)
        assertEquals(18000, plain.pricing.price)
        assertNull(plain.ficha)
        assertFalse(plain.isCombo)
    }

    @Test fun mapsCombo() {
        val combo = decode().first { it.id == "combo-finde" }
        assertTrue(combo.isCombo)
        assertEquals(35000, combo.price)
        assertEquals(2, combo.comboItems?.size)
        assertEquals("aurora", combo.comboItems?.first()?.productId)
        assertEquals(1, combo.comboItems?.first()?.qty)
        assertEquals(2, combo.comboItems?.last()?.qty)
    }

    @Test fun rejectsInvalidDocuments() {
        assertNull(FirestoreCatalogMapper.product(JSONObject()))
        assertNull(FirestoreCatalogMapper.product(JSONObject("""{"name":"a/b/x"}""")))
    }

    @Test fun throwsOnUnreadableJson() {
        var threw = false
        try { FirestoreCatalogMapper.products("no-json") } catch (e: ApiException) { threw = true }
        assertTrue(threw)
    }

    @Test fun parsesRunQueryArrayShape() {
        val json = """
        [
          { "readTime": "2026-06-24T00:00:00Z" },
          { "document": {"name": "projects/x/databases/(default)/documents/products/aurora", "fields": {"name": {"stringValue": "Aurora"}, "price": {"integerValue": "20000"}}}},
          { "document": {"name": "projects/x/databases/(default)/documents/products/plain", "fields": {"name": {"stringValue": "Plano"}, "price": {"integerValue": "18000"}}}}
        ]
        """.trimIndent()
        val products = FirestoreCatalogMapper.products(json)
        assertEquals(listOf("aurora", "plain"), products.map { it.id })
        assertEquals("Aurora", products.first().name)
    }
}

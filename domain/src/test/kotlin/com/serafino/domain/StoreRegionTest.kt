package com.serafino.domain

import com.serafino.domain.services.StoreRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Espeja `StoreRegionTests` de iOS. */
class StoreRegionTest {
    @Test fun argentinaAllows() {
        assertTrue(StoreRegion.allowsStore("AR"))
        assertTrue(StoreRegion.allowsStore("ar"))
        assertTrue(StoreRegion.allowsStore("Ar"))
        assertTrue(StoreRegion.allowsStore("  AR "))
        assertEquals("AR", StoreRegion.countryCode)
    }

    @Test fun othersDeny() {
        assertFalse(StoreRegion.allowsStore("US"))
        assertFalse(StoreRegion.allowsStore("BR"))
        assertFalse(StoreRegion.allowsStore("UY"))
        assertFalse(StoreRegion.allowsStore("ARG"))
        assertFalse(StoreRegion.allowsStore(""))
        assertFalse(StoreRegion.allowsStore("   "))
        assertFalse(StoreRegion.allowsStore(null))
    }
}

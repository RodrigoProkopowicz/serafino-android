package com.serafino.domain

import com.serafino.domain.services.CheckoutForm
import com.serafino.domain.services.CheckoutValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Espeja la validación y la regla de envío gratis de `CheckoutValidator` de iOS. */
class CheckoutValidatorTest {

    @Test fun emptyFormReportsAllFields() {
        assertEquals(7, CheckoutValidator.validate(CheckoutForm()).size)
    }

    @Test fun validFormHasNoErrors() {
        val form = CheckoutForm(
            name = "Ana Pérez", email = "ana@mail.com", phone = "1122334455",
            street = "Piñero 189", city = "San Miguel", province = "Buenos Aires", zip = "1663",
        )
        assertTrue(CheckoutValidator.validate(form).isEmpty())
    }

    @Test fun invalidEmailReported() {
        val form = CheckoutForm(name = "Ana", email = "no-es-mail", phone = "1", street = "x", city = "y", province = "z", zip = "1")
        assertTrue(CheckoutValidator.validate(form).containsKey(com.serafino.domain.services.CheckoutField.Email))
    }

    @Test fun sanMiguelByZip() {
        assertTrue(CheckoutValidator.isSanMiguel(city = "", zip = "1663"))
        assertTrue(CheckoutValidator.isSanMiguel(city = "", zip = "B1663ABC"))
        assertFalse(CheckoutValidator.isSanMiguel(city = "", zip = "1000"))
    }

    @Test fun sanMiguelByCity() {
        assertTrue(CheckoutValidator.isSanMiguel(city = "San Miguel", zip = ""))
        assertTrue(CheckoutValidator.isSanMiguel(city = "Bella Vista", zip = ""))
        assertTrue(CheckoutValidator.isSanMiguel(city = "Muñiz", zip = ""))
        assertFalse(CheckoutValidator.isSanMiguel(city = "La Plata", zip = ""))
    }
}

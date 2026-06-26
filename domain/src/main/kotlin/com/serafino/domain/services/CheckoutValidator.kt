package com.serafino.domain.services

import java.text.Normalizer

/** Campos del formulario de checkout (contacto + envío). Espeja `CheckoutField` de iOS. */
enum class CheckoutField { Name, Email, Phone, Street, City, Province, Zip }

/**
 * Datos de contacto y envío del comprador. Inmutable: las mutaciones devuelven una copia
 * (`with`). Espeja `CheckoutForm` de iOS.
 */
data class CheckoutForm(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val street: String = "",
    val city: String = "",
    val province: String = "",
    val zip: String = "",
) {
    /** Acceso por campo (para bindings genéricos). */
    operator fun get(field: CheckoutField): String = when (field) {
        CheckoutField.Name -> name
        CheckoutField.Email -> email
        CheckoutField.Phone -> phone
        CheckoutField.Street -> street
        CheckoutField.City -> city
        CheckoutField.Province -> province
        CheckoutField.Zip -> zip
    }

    /** Copia con un campo modificado. */
    fun with(field: CheckoutField, value: String): CheckoutForm = when (field) {
        CheckoutField.Name -> copy(name = value)
        CheckoutField.Email -> copy(email = value)
        CheckoutField.Phone -> copy(phone = value)
        CheckoutField.Street -> copy(street = value)
        CheckoutField.City -> copy(city = value)
        CheckoutField.Province -> copy(province = value)
        CheckoutField.Zip -> copy(zip = value)
    }

    /** Copia con todos los campos sin espacios al borde. */
    fun trimmed(): CheckoutForm = CheckoutForm(
        name = name.trim(),
        email = email.trim(),
        phone = phone.trim(),
        street = street.trim(),
        city = city.trim(),
        province = province.trim(),
        zip = zip.trim(),
    )
}

/**
 * Validación del formulario de checkout y regla de envío gratis, portadas desde
 * `web/src/lib/checkout.js`. Espeja `CheckoutValidator` de iOS.
 */
object CheckoutValidator {

    /** Valida los datos necesarios para concretar un envío. Devuelve errores por campo. */
    fun validate(form: CheckoutForm): Map<CheckoutField, String> {
        val errors = mutableMapOf<CheckoutField, String>()
        if (!filled(form.name)) errors[CheckoutField.Name] = "Ingresá tu nombre y apellido."
        if (!filled(form.email) || !isEmail(form.email)) errors[CheckoutField.Email] = "Ingresá un email válido."
        if (!filled(form.phone)) errors[CheckoutField.Phone] = "Ingresá un teléfono de contacto."
        if (!filled(form.street)) errors[CheckoutField.Street] = "Ingresá la calle y el número."
        if (!filled(form.city)) errors[CheckoutField.City] = "Ingresá la localidad."
        if (!filled(form.province)) errors[CheckoutField.Province] = "Ingresá la provincia."
        if (!filled(form.zip)) errors[CheckoutField.Zip] = "Ingresá el código postal."
        return errors
    }

    // Envío propio gratis dentro del partido de San Miguel (PBA).
    private val sanMiguelLocalities = setOf("san miguel", "bella vista", "bellavista", "muniz")
    private val sanMiguelZips = setOf("1661", "1662", "1663")

    /**
     * ¿La dirección entra en el reparto propio gratis? El CP manda cuando está presente
     * (4 dígitos, también del CPA "B1663ABC"); si no, cae al nombre.
     */
    fun isSanMiguel(city: String, zip: String): Boolean {
        val base = zip.filter { it.isDigit() }.take(4)
        if (base.length == 4) return sanMiguelZips.contains(base)
        return sanMiguelLocalities.contains(normalize(city))
    }

    private fun filled(value: String): Boolean = value.trim().isNotEmpty()

    private val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    private fun isEmail(value: String): Boolean = emailRegex.matches(value.trim())

    /** Minúsculas, sin tildes y con espacios colapsados. */
    private fun normalize(value: String): String {
        val deAccented = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return deAccented.lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }
}

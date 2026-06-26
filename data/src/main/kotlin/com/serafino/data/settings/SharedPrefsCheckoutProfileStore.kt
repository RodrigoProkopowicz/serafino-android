package com.serafino.data.settings

import android.content.SharedPreferences
import com.serafino.domain.services.CheckoutForm
import com.serafino.domain.services.CheckoutProfileStoring
import org.json.JSONObject

/**
 * Persiste el perfil de contacto + envío (`CheckoutForm`) en `SharedPreferences` como JSON.
 * Lectura tolerante: si está corrupto, `load()` devuelve `null`. Espeja
 * `UserDefaultsCheckoutProfileStore` de iOS.
 */
class SharedPrefsCheckoutProfileStore(
    private val prefs: SharedPreferences,
) : CheckoutProfileStoring {

    override fun load(): CheckoutForm? {
        val raw = prefs.getString(KEY_PROFILE, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            CheckoutForm(
                name = json.optString("name"),
                email = json.optString("email"),
                phone = json.optString("phone"),
                street = json.optString("street"),
                city = json.optString("city"),
                province = json.optString("province"),
                zip = json.optString("zip"),
            )
        }.getOrNull()
    }

    override fun save(form: CheckoutForm) {
        val json = JSONObject()
            .put("name", form.name)
            .put("email", form.email)
            .put("phone", form.phone)
            .put("street", form.street)
            .put("city", form.city)
            .put("province", form.province)
            .put("zip", form.zip)
        prefs.edit().putString(KEY_PROFILE, json.toString()).apply()
    }

    private companion object {
        const val KEY_PROFILE = "checkout.profile"
    }
}

package com.serafino.app

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.serafino.architecture.AnalyticsEvent
import com.serafino.architecture.AnalyticsTracking

/**
 * Implementación de [AnalyticsTracking] sobre Firebase Analytics. Vive en el app target (el único
 * que importa el SDK); traduce los eventos de dominio a los eventos/parámetros estándar de GA4.
 * Montos en ARS. Espeja `FirebaseAnalyticsService` de iOS.
 */
class FirebaseAnalyticsService(context: Context) : AnalyticsTracking {

    private val analytics = FirebaseAnalytics.getInstance(context.applicationContext)

    override fun track(event: AnalyticsEvent) {
        when (event) {
            is AnalyticsEvent.Screen -> analytics.logEvent(
                FirebaseAnalytics.Event.SCREEN_VIEW,
                bundleOf(FirebaseAnalytics.Param.SCREEN_NAME to event.name),
            )

            is AnalyticsEvent.ViewItem -> {
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.ITEM_ID, event.id)
                    putString(FirebaseAnalytics.Param.ITEM_NAME, event.name)
                    event.value?.let {
                        putLong(FirebaseAnalytics.Param.VALUE, it.toLong())
                        putString(FirebaseAnalytics.Param.CURRENCY, CURRENCY)
                    }
                }
                analytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, params)
            }

            is AnalyticsEvent.AddToCart -> analytics.logEvent(
                FirebaseAnalytics.Event.ADD_TO_CART,
                bundleOf(
                    FirebaseAnalytics.Param.VALUE to event.value.toLong(),
                    FirebaseAnalytics.Param.CURRENCY to CURRENCY,
                    FirebaseAnalytics.Param.ITEMS to arrayOf(
                        bundleOf(
                            FirebaseAnalytics.Param.ITEM_ID to event.id,
                            FirebaseAnalytics.Param.ITEM_NAME to event.name,
                            FirebaseAnalytics.Param.QUANTITY to event.quantity.toLong(),
                        ),
                    ),
                ),
            )

            is AnalyticsEvent.BeginCheckout -> analytics.logEvent(
                FirebaseAnalytics.Event.BEGIN_CHECKOUT,
                bundleOf(
                    FirebaseAnalytics.Param.VALUE to event.value.toLong(),
                    FirebaseAnalytics.Param.CURRENCY to CURRENCY,
                    "item_count" to event.itemCount.toLong(),
                ),
            )

            is AnalyticsEvent.Purchase -> analytics.logEvent(
                FirebaseAnalytics.Event.PURCHASE,
                bundleOf(
                    FirebaseAnalytics.Param.TRANSACTION_ID to event.orderID,
                    FirebaseAnalytics.Param.VALUE to event.value.toLong(),
                    FirebaseAnalytics.Param.CURRENCY to CURRENCY,
                ),
            )

            is AnalyticsEvent.BrewCompleted -> analytics.logEvent(
                "brew_completed",
                bundleOf("recipe_id" to event.recipeID),
            )
        }
    }

    private companion object {
        const val CURRENCY = "ARS"
    }
}

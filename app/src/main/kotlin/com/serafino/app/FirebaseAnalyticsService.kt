package com.serafino.app

import android.content.Context
import android.os.Bundle
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
                Bundle().apply { putString(FirebaseAnalytics.Param.SCREEN_NAME, event.name) },
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
                Bundle().apply {
                    putLong(FirebaseAnalytics.Param.VALUE, event.value.toLong())
                    putString(FirebaseAnalytics.Param.CURRENCY, CURRENCY)
                    putParcelableArray(
                        FirebaseAnalytics.Param.ITEMS,
                        arrayOf(
                            Bundle().apply {
                                putString(FirebaseAnalytics.Param.ITEM_ID, event.id)
                                putString(FirebaseAnalytics.Param.ITEM_NAME, event.name)
                                putLong(FirebaseAnalytics.Param.QUANTITY, event.quantity.toLong())
                            },
                        ),
                    )
                },
            )

            is AnalyticsEvent.BeginCheckout -> analytics.logEvent(
                FirebaseAnalytics.Event.BEGIN_CHECKOUT,
                Bundle().apply {
                    putLong(FirebaseAnalytics.Param.VALUE, event.value.toLong())
                    putString(FirebaseAnalytics.Param.CURRENCY, CURRENCY)
                    putLong("item_count", event.itemCount.toLong())
                },
            )

            is AnalyticsEvent.Purchase -> analytics.logEvent(
                FirebaseAnalytics.Event.PURCHASE,
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.TRANSACTION_ID, event.orderID)
                    putLong(FirebaseAnalytics.Param.VALUE, event.value.toLong())
                    putString(FirebaseAnalytics.Param.CURRENCY, CURRENCY)
                },
            )

            is AnalyticsEvent.BrewCompleted -> analytics.logEvent(
                "brew_completed",
                Bundle().apply { putString("recipe_id", event.recipeID) },
            )
        }
    }

    private companion object {
        const val CURRENCY = "ARS"
    }
}

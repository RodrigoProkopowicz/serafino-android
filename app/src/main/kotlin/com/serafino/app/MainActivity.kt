package com.serafino.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.serafino.architecture.AppDidBecomeActiveEvent
import com.serafino.architecture.EventBus
import com.serafino.architecture.require
import com.serafino.designsystem.SerafinoTheme
import com.serafino.domain.services.StoreRegion

/**
 * Actividad única que monta la UI Compose. Dark-first, edge-to-edge (estilo Apple TV).
 * Espeja la `WindowGroup` de `SerafinoApp` de iOS.
 */
class MainActivity : ComponentActivity() {
    /** Bus compartido: la raíz publica la vuelta al foreground UNA sola vez para toda la app. */
    private lateinit var bus: EventBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SerafinoApplication
        bus = app.container.require<EventBus>()
        val factory = ModuleFactory(app.container)
        // Tienda + Perfil solo en Argentina (región del dispositivo); fuera de AR, solo Recetas.
        val showsStoreSections = StoreRegion.isAvailableForCurrentRegion

        setContent {
            SerafinoTheme {
                RootTab(factory = factory, showsStoreSections = showsStoreSections)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Vuelta al foreground (el análogo de `scenePhase == .active` en iOS): un solo
        // publisher para toda la app; los interactores que cachean datos remotos se
        // suscriben y refrescan solo si su TTL venció.
        bus.publish(AppDidBecomeActiveEvent())
    }
}

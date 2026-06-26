package com.serafino.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.serafino.designsystem.SerafinoTheme
import com.serafino.domain.services.StoreRegion

/**
 * Actividad única que monta la UI Compose. Dark-first, edge-to-edge (estilo Apple TV).
 * Espeja la `WindowGroup` de `SerafinoApp` de iOS.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SerafinoApplication
        val factory = ModuleFactory(app.container)
        // Tienda + Perfil solo en Argentina (región del dispositivo); fuera de AR, solo Recetas.
        val showsStoreSections = StoreRegion.isAvailableForCurrentRegion

        setContent {
            SerafinoTheme {
                RootTab(factory = factory, showsStoreSections = showsStoreSections)
            }
        }
    }
}

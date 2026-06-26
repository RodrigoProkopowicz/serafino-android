package com.serafino.data.di

import com.serafino.architecture.DependencyContainer
import com.serafino.architecture.DependencyModule
import com.serafino.architecture.DependencyScope
import com.serafino.architecture.register
import com.serafino.data.loyalty.SampleLoyaltyProvider
import com.serafino.domain.services.LoyaltyProviding

/**
 * Registra la fuente del programa de fidelidad con el provider de muestra (dev/preview, sin red).
 * El composition root puede sobreescribirlo con el backend real. Espeja `LoyaltyModule` de iOS.
 */
class LoyaltyModule : DependencyModule {
    override fun register(container: DependencyContainer) {
        container.register<LoyaltyProviding>(DependencyScope.Singleton) { SampleLoyaltyProvider() }
    }
}

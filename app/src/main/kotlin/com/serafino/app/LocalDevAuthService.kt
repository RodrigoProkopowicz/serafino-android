package com.serafino.app

import com.serafino.architecture.AuthChangedEvent
import com.serafino.architecture.AuthProviding
import com.serafino.architecture.AuthUser
import com.serafino.architecture.EventBus
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Servicio de sesión local de desarrollo: simula el inicio de sesión para recorrer el flujo de
 * fidelidad sin un backend de auth configurado (análogo al `LocalDevAuthService` de iOS en DEBUG).
 */
class LocalDevAuthService(private val bus: EventBus) : AuthProviding {
    private var user: AuthUser? = null

    override val currentUser: AuthUser? get() = user

    override suspend fun signIn() {
        delay(600)
        user = AuthUser(
            uid = "dev-user",
            displayName = "Miembro Serafino",
            email = "miembro@serafino.coffee",
            memberSince = memberSince,
        )
        bus.publish(AuthChangedEvent(isSignedIn = true))
    }

    override fun signOut() {
        user = null
        bus.publish(AuthChangedEvent(isSignedIn = false))
    }

    override suspend fun idToken(): String? = null

    private companion object {
        val memberSince = Calendar.getInstance().apply { clear(); set(2025, Calendar.MARCH, 1) }.time
    }
}

package com.serafino.architecture

import java.util.Date

/**
 * Contrato de autenticación de la app, en Architecture para que los features lo usen por
 * inversión de dependencias SIN conocer Firebase ni el proveedor. La implementación real vive
 * en el app target. Espeja `AuthProviding` de iOS.
 */

/** Usuario autenticado (proyección mínima y agnóstica del proveedor). */
data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    /** Fecha de alta de la cuenta (metadata del proveedor). Alimenta "Miembro desde…". */
    val memberSince: Date? = null,
)

/** Errores de autenticación con mensaje listo para mostrar. */
sealed class AuthError(val displayMessage: String) : Exception(displayMessage) {
    /** Falta configurar el backend de auth — no se puede autenticar. */
    data object Unavailable : AuthError("El inicio de sesión no está disponible todavía.")
    /** El usuario canceló la hoja de inicio de sesión. */
    data object Cancelled : AuthError("Cancelaste el inicio de sesión.")
    /** Falló el inicio de sesión (con detalle del proveedor). */
    data class Failed(val reason: String) : AuthError(reason)
}

/**
 * Sesión de la app. La implementación real usa el proveedor de inicio de sesión + el backend.
 * Para reaccionar a los cambios, suscribirse a [AuthChangedEvent] y releer [currentUser].
 */
interface AuthProviding {
    /** Usuario actual, o `null` si no hay sesión. */
    val currentUser: AuthUser?

    /** Inicia sesión (presenta la hoja del sistema). Lanza [AuthError] si falla o se cancela. */
    suspend fun signIn()

    /** Cierra la sesión actual. */
    fun signOut()

    /** ID token del usuario actual (se refresca si venció), o `null` si no hay sesión. */
    suspend fun idToken(): String?
}

/** Implementación nula por defecto: sin sesión y sin capacidad de autenticar. */
class NoOpAuthService : AuthProviding {
    override val currentUser: AuthUser? = null
    override suspend fun signIn(): Unit = throw AuthError.Unavailable
    override fun signOut() {}
    override suspend fun idToken(): String? = null
}

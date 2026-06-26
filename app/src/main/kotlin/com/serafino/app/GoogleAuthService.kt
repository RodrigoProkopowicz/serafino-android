package com.serafino.app

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.serafino.architecture.AuthChangedEvent
import com.serafino.architecture.AuthError
import com.serafino.architecture.AuthProviding
import com.serafino.architecture.AuthUser
import com.serafino.architecture.EventBus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Implementación de [AuthProviding] sobre **Sign in with Google + Firebase Auth**, usando
 * **Credential Manager** (la API moderna basada en corrutinas). Vive en el app target (el único que
 * importa el SDK). Presenta el selector de cuentas de Google, canjea el ID token por una sesión de
 * Firebase ([GoogleAuthProvider]) y mantiene [currentUser] en sync con el listener de estado,
 * publicando [AuthChangedEvent]. Reemplaza al login de Apple del puerto iOS por el nativo de Android.
 *
 * El `serverClientId` es el Web client ID del proyecto Firebase, que el plugin google-services
 * expone como `R.string.default_web_client_id` a partir de `app/google-services.json`.
 *
 * PENDIENTE del lado plataforma para que funcione de verdad: habilitar Google como proveedor en
 * Firebase console (Authentication → Sign-in method) y registrar el SHA-1/SHA-256 del paquete
 * `com.rodrigoprokopowicz.serafino` en el proyecto Firebase.
 */
class GoogleAuthService(
    context: Context,
    private val bus: EventBus,
    private val activityProvider: () -> Activity?,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AuthProviding {

    private val appContext = context.applicationContext
    private val credentialManager = CredentialManager.create(appContext)
    private val serverClientId = appContext.getString(R.string.default_web_client_id)

    @Volatile
    override var currentUser: AuthUser? = auth.currentUser?.toAuthUser()
        private set

    private val stateListener = FirebaseAuth.AuthStateListener { fa ->
        currentUser = fa.currentUser?.toAuthUser()
        bus.publish(AuthChangedEvent(isSignedIn = fa.currentUser != null))
    }

    init {
        // Estado inicial + cambios (login/logout/restauración de sesión persistida).
        auth.addAuthStateListener(stateListener)
    }

    override suspend fun signIn() {
        val activity = activityProvider()
            ?: throw AuthError.Failed("No hay una pantalla activa para iniciar sesión.")
        // `filterByAuthorizedAccounts = false`: permite elegir cualquier cuenta de Google del
        // dispositivo (no solo las que ya autorizaron la app), el flujo típico de "iniciar sesión".
        val googleOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build()
        try {
            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                auth.signInWithCredential(firebaseCredential).await()
                // El state listener ya actualizó currentUser y publicó AuthChangedEvent.
            } else {
                throw AuthError.Failed("Credencial de Google inesperada.")
            }
        } catch (e: GetCredentialCancellationException) {
            throw AuthError.Cancelled
        } catch (e: NoCredentialException) {
            throw AuthError.Failed("No hay cuentas de Google disponibles en este dispositivo.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: AuthError) {
            throw e
        } catch (e: Exception) {
            throw AuthError.Failed(e.localizedMessage ?: "No se pudo completar el inicio de sesión con Google.")
        }
    }

    override fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            throw AuthError.Failed(e.localizedMessage ?: "No se pudo cerrar la sesión.")
        }
    }

    /** ID token de Firebase del usuario actual (se refresca si venció), o `null` si no hay sesión. */
    override suspend fun idToken(): String? {
        val user = auth.currentUser ?: return null
        return user.getIdToken(false).await().token
    }
}

/** Proyecta el usuario de Firebase al value type de dominio (incluye la fecha de alta). */
private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
    uid = uid,
    displayName = displayName,
    email = email,
    memberSince = metadata?.creationTimestamp?.let { Date(it) },
)

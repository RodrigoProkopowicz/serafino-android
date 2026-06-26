package com.serafino.app

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.google.firebase.FirebaseApp
import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.AuthProviding
import com.serafino.architecture.DependencyContainer
import com.serafino.architecture.DependencyScope
import com.serafino.architecture.EventBus
import com.serafino.architecture.NoOpAnalytics
import com.serafino.architecture.NoOpAuthService
import com.serafino.architecture.register
import com.serafino.architecture.require
import com.serafino.data.di.CoreModule
import com.serafino.data.di.LoyaltyModule
import com.serafino.data.di.StoreModule
import com.serafino.data.loyalty.BackendLoyaltyProvider
import com.serafino.designsystem.Haptics
import com.serafino.domain.services.LoyaltyProviding

/**
 * Composition root: arma el [DependencyContainer], registra módulos y servicios, y configura los
 * hápticos antes de pasar el control al `ModuleFactory`. Espeja `SerafinoApp` de iOS.
 *
 * La disponibilidad del backend se decide EN RUNTIME a partir de si Firebase quedó inicializado
 * (presencia de `app/google-services.json`, que el plugin google-services procesa). Igual que iOS,
 * que mira `FirebaseApp.app() != nil`: agregás el archivo de config y todo se activa solo, sin
 * tocar más código.
 */
class SerafinoApplication : Application() {

    lateinit var container: DependencyContainer
        private set

    /** Activity en primer plano (para que el flujo de inicio de sesión web tenga dónde presentarse). */
    @Volatile
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(ActivityTracker())

        // ¿Firebase inicializado? El plugin google-services lo auto-inicializa vía FirebaseInitProvider
        // cuando hay un `google-services.json` válido en `app/`. Sin el archivo, queda en `false` y se
        // usan los servicios locales/muestra — igual que la app iOS sin `GoogleService-Info.plist`.
        val firebaseConfigured = FirebaseApp.getApps(this).isNotEmpty()

        container = DependencyContainer().apply {
            register(
                listOf(
                    CoreModule(this@SerafinoApplication),
                    StoreModule(this@SerafinoApplication),
                    LoyaltyModule(),
                ),
            )
            // Analítica: con Firebase configurado, GA4 real; si no, NoOp (los features la resuelven por
            // protocolo). Espeja `FirebaseAnalyticsService` de iOS.
            register<AnalyticsTracking>(DependencyScope.Singleton) {
                if (firebaseConfigured) FirebaseAnalyticsService(this@SerafinoApplication) else NoOpAnalytics()
            }

            // Sesión: Sign in with Google + Firebase Auth si está configurado; si no, en DEBUG el
            // servicio local que simula el login (para recorrer Perfil/fidelidad) y en release
            // `NoOpAuthService`. Mismo branching que iOS (Firebase → DEBUG → NoOp), con Google en
            // lugar de Apple (el inicio de sesión nativo de Android).
            register<AuthProviding>(DependencyScope.Singleton) { resolver ->
                when {
                    firebaseConfigured -> GoogleAuthService(
                        context = this@SerafinoApplication,
                        bus = resolver.require<EventBus>(),
                        activityProvider = { currentActivity },
                    )
                    BuildConfig.DEBUG -> LocalDevAuthService(resolver.require<EventBus>())
                    else -> NoOpAuthService()
                }
            }

            // Puntos: con Firebase configurado, la fuente real es `BackendLoyaltyProvider` (API
            // `/api/loyalty/*` con el ID token); sobreescribe el `SampleLoyaltyProvider` que registró
            // LoyaltyModule. Sin backend, queda el de muestra. Espeja el override condicional de iOS.
            if (firebaseConfigured) {
                register<LoyaltyProviding>(DependencyScope.Singleton) { resolver ->
                    BackendLoyaltyProvider(auth = resolver.require<AuthProviding>())
                }
            }
        }

        Haptics.performer = AndroidHaptics(this)
    }

    /** Mantiene [currentActivity] en sync con la Activity en primer plano. */
    private inner class ActivityTracker : ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) { currentActivity = activity }
        override fun onActivityPaused(activity: Activity) {
            if (currentActivity === activity) currentActivity = null
        }
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }
}

/** Implementación de [Haptics.Performer] con el `Vibrator` del sistema. */
private class AndroidHaptics(application: Application) : Haptics.Performer {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (application.getSystemService(Vibrator::class.java)?.let { it })
            ?: (application.getSystemService(VibratorManager::class.java))?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Vibrator::class.java)
    }

    private fun buzz(ms: Long, amplitude: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createOneShot(ms, amplitude))
    }

    override fun tap() = buzz(12, 90)
    override fun step() = buzz(18, 160)
    override fun success() = buzz(28, 220)
}

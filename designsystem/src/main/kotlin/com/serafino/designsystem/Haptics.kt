package com.serafino.designsystem

/**
 * Pequeño envoltorio de feedback háptico. Respeta la preferencia del usuario (el llamador decide
 * si está habilitado). En iOS usa `UIImpactFeedbackGenerator`; acá delega en un [Performer] que
 * la app instala con el `Vibrator`/`View` del sistema (default no-op para tests/previews).
 * Espeja `Haptics` de iOS.
 */
object Haptics {
    interface Performer {
        fun tap()
        fun step()
        fun success()
    }

    private object NoOp : Performer {
        override fun tap() {}
        override fun step() {}
        override fun success() {}
    }

    @Volatile
    var performer: Performer = NoOp

    fun tap() = performer.tap()
    fun step() = performer.step()
    fun success() = performer.success()
}

package com.serafino.feature.recipes.timer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.serafino.architecture.AnalyticsEvent
import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.BrewCompletedEvent
import com.serafino.architecture.EventBus
import com.serafino.architecture.Interactor
import com.serafino.architecture.NoOpAnalytics
import com.serafino.designsystem.Haptics
import com.serafino.domain.entities.BrewMethod
import com.serafino.domain.entities.BrewStep
import com.serafino.domain.services.BrewUnitFormatter
import com.serafino.domain.services.RecipeCatalogProviding
import com.serafino.domain.services.SettingsStoring
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Conduce el timer de preparación paso a paso. Los pasos cronometrados cuentan regresivo; los
 * manuales esperan al usuario. El ticker es una corrutina en Main (sin hops de actor).
 * Espeja `BrewTimerInteractor` de iOS.
 */
class BrewTimerInteractor(
    private val recipeID: String,
    private val catalog: RecipeCatalogProviding,
    private val settings: SettingsStoring,
    private val bus: EventBus,
    private val analytics: AnalyticsTracking = NoOpAnalytics(),
) : Interactor<BrewTimerInteractor.Data, BrewTimerInteractor.Input, BrewTimerInteractor.State> {

    data class Data(
        val recipeName: String = "",
        val method: BrewMethod = BrewMethod.Espresso,
        val totalSteps: Int = 0,
        val currentIndex: Int = 0,
        val stepCountText: String = "",
        val stepTitle: String = "",
        val stepDetail: String = "",
        val isTimedStep: Boolean = false,
        val remainingText: String = "",
        val stepProgress: Double = 0.0,
        val waterTargetText: String? = null,
        val manualHint: String? = null,
        val isRunning: Boolean = false,
        val isFinished: Boolean = false,
    )

    sealed interface Input {
        data object OnAppear : Input
        data object OnDisappear : Input
        data object TogglePrimary : Input
        data object NextStep : Input
        data object PreviousStep : Input
        data object Restart : Input
        data object Tick : Input
    }

    enum class State { Ready, Running, Paused, Finished }

    override var data by mutableStateOf(Data())
        private set
    override var state by mutableStateOf(State.Ready)
        private set

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var steps: List<BrewStep> = emptyList()
    private var index = 0
    private var ticker: Job? = null
    private var didLoad = false

    override fun handle(input: Input) {
        when (input) {
            is Input.OnAppear -> load()
            is Input.OnDisappear -> pause()
            is Input.TogglePrimary -> togglePrimary()
            is Input.NextStep -> advance(auto = false)
            is Input.PreviousStep -> goBack()
            is Input.Restart -> restart()
            is Input.Tick -> tick()
        }
    }

    fun dispose() {
        ticker?.cancel()
        scope.cancel()
    }

    private fun load() {
        if (didLoad) return
        didLoad = true
        val recipe = catalog.recipe(recipeID) ?: return
        steps = recipe.steps
        index = 0
        data = data.copy(recipeName = recipe.name, method = recipe.method, totalSteps = steps.size)
        configureCurrent()
        state = State.Ready
    }

    private fun configureCurrent() {
        val step = steps.getOrNull(index) ?: return
        val formatter = makeFormatter()
        data = data.copy(
            currentIndex = index,
            stepCountText = "Paso ${index + 1} de ${data.totalSteps}",
            stepTitle = step.title,
            stepDetail = step.detail,
            isTimedStep = step.isTimed,
            waterTargetText = step.waterTarget?.let { "Verté hasta " + formatter.mass(it.toDouble()) },
            manualHint = step.manualHint,
            remainingText = step.duration?.let { formatter.duration(it) } ?: "",
            stepProgress = 0.0,
        )
    }

    private fun togglePrimary() {
        if (data.isFinished) { restart(); return }
        if (data.isTimedStep) {
            if (data.isRunning) pause() else start()
        } else {
            advance(auto = false)
        }
    }

    private fun start() {
        if (!data.isTimedStep) return
        data = data.copy(isRunning = true)
        state = State.Running
        startTicker()
    }

    private fun pause() {
        data = data.copy(isRunning = false)
        if (state != State.Finished) state = State.Paused
        ticker?.cancel()
        ticker = null
    }

    private fun tick() {
        if (!data.isRunning || !data.isTimedStep) return
        val duration = steps.getOrNull(index)?.duration ?: return

        val elapsed = (data.stepProgress * duration).roundToInt() + 1
        if (elapsed >= duration) {
            data = data.copy(stepProgress = 1.0, remainingText = makeFormatter().duration(0))
            if (settings.hapticsEnabled) Haptics.step()
            advance(auto = true)
        } else {
            data = data.copy(
                stepProgress = elapsed.toDouble() / duration,
                remainingText = makeFormatter().duration(duration - elapsed),
            )
        }
    }

    private fun advance(auto: Boolean) {
        val keepRunning = auto && data.isRunning
        if (index + 1 >= steps.size) {
            finish()
            return
        }
        index += 1
        configureCurrent()
        if (keepRunning && data.isTimedStep) {
            data = data.copy(isRunning = true)
            state = State.Running
            // el ticker sigue corriendo hacia el próximo paso cronometrado
        } else {
            pause()
        }
    }

    private fun goBack() {
        pause()
        index = maxOf(0, index - 1)
        data = data.copy(isFinished = false)
        configureCurrent()
        state = State.Ready
    }

    private fun restart() {
        pause()
        index = 0
        data = data.copy(isFinished = false)
        configureCurrent()
        state = State.Ready
    }

    private fun finish() {
        ticker?.cancel()
        ticker = null
        data = data.copy(isRunning = false, isFinished = true, stepProgress = 1.0)
        state = State.Finished
        if (settings.hapticsEnabled) Haptics.success()
        analytics.track(AnalyticsEvent.BrewCompleted(recipeID))
        bus.publish(BrewCompletedEvent(recipeID))
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(1000)
                handle(Input.Tick)
            }
        }
    }

    private fun makeFormatter() = BrewUnitFormatter(settings.measurementSystem, settings.temperatureUnit)
}

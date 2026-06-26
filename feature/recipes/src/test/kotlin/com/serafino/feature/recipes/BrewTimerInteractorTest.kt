package com.serafino.feature.recipes

import com.serafino.architecture.BrewCompletedEvent
import com.serafino.architecture.EventBus
import com.serafino.architecture.events
import com.serafino.feature.recipes.timer.BrewTimerInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * El ticker se maneja determinísticamente enviando `Tick` directo (sin esperar tiempo real); el
 * ticker interno queda suspendido en su `delay`. Espeja `BrewTimerInteractorTests` de iOS.
 */
class BrewTimerInteractorTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun make(id: String): Pair<BrewTimerInteractor, EventBus> {
        val bus = EventBus()
        val interactor = BrewTimerInteractor(id, MockRecipeCatalog(), MockSettingsStore(bus), bus)
        return interactor to bus
    }

    @Test fun loadsFirstStep() {
        val (interactor, _) = make("test-espresso")
        interactor.handle(BrewTimerInteractor.Input.OnAppear)
        assertEquals(BrewTimerInteractor.State.Ready, interactor.state)
        assertEquals(2, interactor.data.totalSteps)
        assertEquals(0, interactor.data.currentIndex)
        assertEquals("Tamp", interactor.data.stepTitle)
        assertFalse(interactor.data.isTimedStep) // primer paso espresso es manual
    }

    @Test fun manualStepAdvances() {
        val (interactor, _) = make("test-espresso")
        interactor.handle(BrewTimerInteractor.Input.OnAppear)
        interactor.handle(BrewTimerInteractor.Input.TogglePrimary) // manual → next
        assertEquals(1, interactor.data.currentIndex)
        assertTrue(interactor.data.isTimedStep)
        assertFalse(interactor.data.isRunning)
    }

    @Test fun timedRunCompletes() {
        val (interactor, bus) = make("test-v60")
        val completed = mutableListOf<String>()
        val scope = CoroutineScope(mainRule.dispatcher)
        scope.launch { bus.events<BrewCompletedEvent>().collect { completed.add(it.recipeID) } }

        interactor.handle(BrewTimerInteractor.Input.OnAppear)
        interactor.handle(BrewTimerInteractor.Input.TogglePrimary) // start
        assertEquals(BrewTimerInteractor.State.Running, interactor.state)

        interactor.handle(BrewTimerInteractor.Input.Tick) // step 1: 1/2
        assertEquals("0:01", interactor.data.remainingText)
        interactor.handle(BrewTimerInteractor.Input.Tick) // step 1 done → auto-advance
        assertEquals(1, interactor.data.currentIndex)
        assertTrue(interactor.data.isRunning)

        interactor.handle(BrewTimerInteractor.Input.Tick) // step 2: 1/2
        interactor.handle(BrewTimerInteractor.Input.Tick) // step 2 done → finish

        assertEquals(BrewTimerInteractor.State.Finished, interactor.state)
        assertTrue(interactor.data.isFinished)
        assertEquals(listOf("test-v60"), completed)
        scope.cancel()
    }

    @Test fun pauseStopsWithoutFinishing() {
        val (interactor, _) = make("test-v60")
        interactor.handle(BrewTimerInteractor.Input.OnAppear)
        interactor.handle(BrewTimerInteractor.Input.TogglePrimary) // start
        assertTrue(interactor.data.isRunning)
        interactor.handle(BrewTimerInteractor.Input.TogglePrimary) // pause
        assertFalse(interactor.data.isRunning)
        assertEquals(BrewTimerInteractor.State.Paused, interactor.state)
        assertFalse(interactor.data.isFinished)
    }

    @Test fun previousStepResetsRunning() {
        val (interactor, _) = make("test-v60")
        interactor.handle(BrewTimerInteractor.Input.OnAppear)
        interactor.handle(BrewTimerInteractor.Input.NextStep)
        assertEquals(1, interactor.data.currentIndex)
        interactor.handle(BrewTimerInteractor.Input.PreviousStep)
        assertEquals(0, interactor.data.currentIndex)
        assertFalse(interactor.data.isRunning)
    }

    @Test fun nextPastEndFinishes() {
        val (interactor, _) = make("test-fp")
        interactor.handle(BrewTimerInteractor.Input.OnAppear)
        interactor.handle(BrewTimerInteractor.Input.NextStep)
        assertEquals(BrewTimerInteractor.State.Finished, interactor.state)
        assertTrue(interactor.data.isFinished)
    }

    @Test fun restartReturnsToFirstStep() {
        val (interactor, _) = make("test-v60")
        interactor.handle(BrewTimerInteractor.Input.OnAppear)
        interactor.handle(BrewTimerInteractor.Input.NextStep)
        interactor.handle(BrewTimerInteractor.Input.NextStep) // finishes
        interactor.handle(BrewTimerInteractor.Input.Restart)
        assertEquals(0, interactor.data.currentIndex)
        assertFalse(interactor.data.isFinished)
        assertEquals(BrewTimerInteractor.State.Ready, interactor.state)
    }
}

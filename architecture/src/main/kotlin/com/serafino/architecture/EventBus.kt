package com.serafino.architecture

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Marca un evento publicable a través del [EventBus]. Cada evento se modela como un tipo
 * (data class / sealed) que implementa esta interfaz.
 */
interface BusEvent

/**
 * Bus de eventos centralizado, type-safe, basado en `SharedFlow` (el análogo del `EventBus`
 * de iOS basado en Combine `PassthroughSubject`).
 *
 * - Sin replay: los colectores reciben solo lo que se publique después de suscribirse.
 * - Thread-safe.
 * - Reusable: no hay que tocar el bus para agregar nuevos eventos.
 *
 * Uso típico:
 * 1. Definir eventos como tipos que implementen [BusEvent].
 * 2. Publicar con `bus.publish(MyEvent(...))`.
 * 3. Suscribirse colectando `bus.events<MyEvent>()` en un `CoroutineScope`.
 */
class EventBus {

    // replay 0 (sin estado replay) + buffer holgado y DROP_OLDEST para que `publish` nunca
    // se bloquee ni suspenda, igual que `subject.send(_:)` de Combine.
    private val flow = MutableSharedFlow<BusEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Publica un evento a todos los suscriptores de su tipo. Sin colectores, se descarta. */
    fun publish(event: BusEvent) {
        flow.tryEmit(event)
    }

    /** Flujo de todos los eventos del tipo indicado. */
    @Suppress("UNCHECKED_CAST")
    fun <E : BusEvent> events(type: Class<E>): Flow<E> =
        flow.filter { type.isInstance(it) }.map { it as E }

    companion object {
        val shared = EventBus()
    }
}

/** Azúcar reificado: `bus.events<MyEvent>()`. */
inline fun <reified E : BusEvent> EventBus.events(): Flow<E> = events(E::class.java)

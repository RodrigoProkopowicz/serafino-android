package com.serafino.architecture

/**
 * Contrato que TODO interactor de la app debe cumplir (la "I" de VIPER). Asegura un patrón
 * consistente entre módulos, espejando el `InteractorType` de iOS:
 *
 * - [Data]:  los datos que la vista renderiza.
 * - [Input]: las acciones que el presenter puede disparar.
 * - [State]: los estados posibles del módulo.
 *
 * Las implementaciones respaldan [data] y [state] con Compose snapshot state
 * (`mutableStateOf`), el análogo fiel del `@Observable` de SwiftUI: la UI lee estas
 * propiedades en el `body`/composable y se recompone sola cuando cambian.
 */
interface Interactor<Data, Input, State> {
    /** Snapshot actual de los datos de la vista. Observable. */
    val data: Data

    /** Estado actual del módulo. Observable. */
    val state: State

    /** Punto de entrada único para todas las acciones. El presenter lo invoca. */
    fun handle(input: Input)
}

/**
 * La "P" de VIPER. Traduce los intents de la vista en `Interactor.Input` y delega la
 * navegación en el [Router]. Es el único objeto que la vista conoce.
 */
interface Presenter<I> {
    val interactor: I
}

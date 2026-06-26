package com.serafino.architecture

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KClass

/**
 * Ciclo de vida de una dependencia registrada.
 * - [Singleton]: la instancia se crea una vez y se reutiliza en cada `resolve`.
 * - [Transient]: se crea una instancia nueva en cada `resolve`.
 */
enum class DependencyScope { Singleton, Transient }

/**
 * Agrupa registros de dependencias relacionados por feature (`CoreModule`, `StoreModule`, …).
 */
interface DependencyModule {
    fun register(container: DependencyContainer)
}

/**
 * Contenedor de inyección de dependencias thread-safe. Resolución por tipo, scopes
 * singleton/transient y composición vía [DependencyModule]. Espeja el `DependencyContainer`
 * de iOS.
 *
 * Usa un [ReentrantLock] (equivalente al `NSRecursiveLock` de iOS): `resolve` mantiene el lock
 * mientras corre el factory y los factories pueden resolver sus propias dependencias
 * (resolución anidada); un lock no reentrante deadlockearía el hilo.
 */
class DependencyContainer {

    private class Registration(
        val scope: DependencyScope,
        val factory: (DependencyContainer) -> Any,
    )

    private val registrations = HashMap<KClass<*>, Registration>()
    private val singletons = HashMap<KClass<*>, Any>()
    private val lock = ReentrantLock()

    /** Registra un tipo con su factory y scope (default [DependencyScope.Transient]). */
    fun <T : Any> register(
        type: KClass<T>,
        scope: DependencyScope = DependencyScope.Transient,
        factory: (DependencyContainer) -> T,
    ) = lock.withLock {
        registrations[type] = Registration(scope) { container -> factory(container) }
        singletons.remove(type)
    }

    /** Aplica todos los [DependencyModule] provistos. */
    fun register(modules: List<DependencyModule>) {
        modules.forEach { it.register(this) }
    }

    /** Resuelve una instancia para el tipo solicitado, o `null` si no fue registrado. */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(type: KClass<T>): T? = lock.withLock {
        val registration = registrations[type] ?: return null
        when (registration.scope) {
            DependencyScope.Singleton -> {
                (singletons[type] as? T)?.let { return it }
                val instance = registration.factory(this)
                singletons[type] = instance
                instance as? T
            }
            DependencyScope.Transient -> registration.factory(this) as? T
        }
    }

    /** Variante de [resolve] que aborta si la dependencia no está registrada. */
    fun <T : Any> require(type: KClass<T>): T =
        resolve(type) ?: error("DependencyContainer: tipo $type no registrado")

    /** Limpia todos los registros y singletons. Pensado para tests. */
    fun reset() = lock.withLock {
        registrations.clear()
        singletons.clear()
    }

    companion object {
        val shared = DependencyContainer()
    }
}

/** Azúcar reificado: `container.resolve<Foo>()`. */
inline fun <reified T : Any> DependencyContainer.resolve(): T? = resolve(T::class)

/** Azúcar reificado: `container.require<Foo>()`. */
inline fun <reified T : Any> DependencyContainer.require(): T = require(T::class)

/** Azúcar reificado para registrar: `container.register<Foo>(scope) { ... }`. */
inline fun <reified T : Any> DependencyContainer.register(
    scope: DependencyScope = DependencyScope.Transient,
    noinline factory: (DependencyContainer) -> T,
) = register(T::class, scope, factory)

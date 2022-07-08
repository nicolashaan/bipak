package fr.haan.bipak.test.util

import app.cash.turbine.FlowTurbine
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.reflect.KClass

/**
 * Wait for an item corresponding to the given predicate
 * @param predicate return true if item matched expectation, false otherwise
 */
public suspend inline fun <reified T> FlowTurbine<T>.awaitForItem(predicate: (T?) -> Boolean): T {
    // Tests have there own timeout, expectItem() can throw TimeoutCancellationException
    var lastItem: T? = null
    try {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
            lastItem = item
        }
    } catch (e: TimeoutCancellationException) {
        println(("Timeout in awaitForItem()! Last received item: $lastItem"))
        throw e
    }
}

/**
 * Wait for an item corresponding to the given type
 */
public suspend inline fun <T, reified U : Any> FlowTurbine<T>.awaitForItemIs(type: KClass<U>): U {
    // Tests have there own timeout, expectItem() can throw TimeoutCancellationException
    var lastItem: T? = null
    try {
        while (true) {
            val item = awaitItem()

            if (type.isInstance(item)) return item as U
            lastItem = item
        }
    } catch (e: TimeoutCancellationException) {
        println(("Timeout in awaitForItemIs()! Last received item: $lastItem"))
        throw e
    }
}

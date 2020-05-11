/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.core.util

import com.gitlab.ykrasik.gamedex.core.CoreEvent
import com.gitlab.ykrasik.gamedex.core.task.ExpectedException
import com.gitlab.ykrasik.gamedex.util.Modifier
import com.gitlab.ykrasik.gamedex.util.logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

/**
 * User: ykrasik
 * Date: 23/05/2020
 * Time: 10:15
 */

val log = logger("")

open class FlowScope(override val coroutineContext: CoroutineContext, open val baseDebugName: String) : CoroutineScope {
    // TODO: Make debugName mandatory
    inline fun <T> Flow<T>.forEach(
        debugName: String? = null,
        traceValues: Boolean = true,
        crossinline f: suspend (T) -> Unit
    ) = launch(
        CoroutineName("$baseDebugName${if (debugName != null) ".$debugName" else ""}"),
        start = CoroutineStart.UNDISPATCHED
    ) {
        try {
            collect {
                try {
                    if (traceValues && it !is Collection<*> && it !is ListEvent<*> && it !is CoreEvent) log.trace(it.toString())
                    f(it)
                } catch (e: Exception) {
                    when (e) {
                        is ExpectedException -> log.trace("Expected exception", e)
                        is CancellationException -> throw e
                        else -> coroutineContext[CoroutineExceptionHandler]?.handleException(Dispatchers.Main.immediate, e) ?: throw e
                    }
                }
            }
        } catch (ignored: CancellationException) {
            log.trace("Cancelled.")
        } catch (e: Exception) {
            log.error("Error", e)
            throw e
        } finally {
            log.trace("Finished.")
        }
    }

    inline fun <T> FlowWithDebugInfo<T>.forEach(crossinline f: suspend (T) -> Unit) = forEach(debugName, traceValues, f)

    inline fun <T> MutableStateFlow<T>.modify(f: Modifier<T>) {
        value = f(value)
    }

    operator fun <T> StateFlow<T>.getValue(thisRef: Any, property: KProperty<*>): T = value
    operator fun <T> MutableStateFlow<T>.setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
    }

    // TODO: Make this a different operator... modAssign? divAssign?
    operator fun <T> MutableStateFlow<T>.timesAssign(value: T) {
        this.value = value
    }

    operator fun <T> MutableStateFlow<T>.timesAssign(flow: Flow<T>) {
        this.bind(flow)
    }

    operator fun <T> MutableStateFlow<T>.timesAssign(flow: FlowWithDebugInfo<T>) {
        this.bind(flow, flow.debugName, flow.traceValues)
    }

    operator fun <T> KProperty0<MutableStateFlow<T>>.timesAssign(flow: Flow<T>) {
        this.get().bind(flow, this.name)
    }

    fun <T> MutableStateFlow<T>.bind(flow: Flow<T>, debugName: String? = null, traceValues: Boolean = true) = flow.forEach(debugName, traceValues) { value = it }

    fun <T> MutableStateFlow<T>.bindBidirectional(flow: MutableStateFlow<T>) {
        flow.forEach { value = it }
        this.drop(1).forEach { flow.value = it }
    }

    infix fun <T> Flow<T>.withDebugName(debugName: String): FlowWithDebugInfo<T> = withDebugInfo(debugName, traceValues = true)
    infix fun <T> Flow<T>.withDebugNameWithoutTrace(debugName: String): FlowWithDebugInfo<T> = withDebugInfo(debugName, traceValues = false)
    fun <T> Flow<T>.withDebugInfo(flow: FlowWithDebugInfo<T>): FlowWithDebugInfo<T> = withDebugInfo(flow.debugName, flow.traceValues)
    fun <T> Flow<T>.withDebugInfo(debugName: String, traceValues: Boolean): FlowWithDebugInfo<T> =
        FlowWithDebugInfoImpl(this, debugName, traceValues)


    infix fun Flow<Boolean>.and(flow: Flow<Boolean>) = this.combine(flow) { thisValue, otherValue -> thisValue && otherValue }

    private fun <T> Flow<T>.asChannel(): ReceiveChannel<T> = produce(capacity = Channel.CONFLATED) {
        collect { send(it) }
    }
}

inline fun Any.flowScope(context: CoroutineContext, crossinline f: FlowScope.() -> Unit) =
    object : FlowScope(context, baseDebugName = this.javaClass.simpleName) {
        init {
            f()
        }
    }

interface FlowWithDebugInfo<T> : Flow<T> {
    val debugName: String
    val traceValues: Boolean
}

private class FlowWithDebugInfoImpl<T>(
    flow: Flow<T>,
    override val debugName: String,
    override val traceValues: Boolean
) : Flow<T> by flow, FlowWithDebugInfo<T>
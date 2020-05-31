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

package com.gitlab.ykrasik.gamedex.app.api.util

import com.gitlab.ykrasik.gamedex.util.IsValid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

fun <T> broadcastFlow() = BroadcastFlow<T>(BroadcastChannel(2))

class BroadcastFlow<T> private constructor(
    private val channel: BroadcastChannel<T>,
    private val flow: Flow<T>
) : BroadcastChannel<T> by channel, Flow<T> by flow {
    companion object {
        operator fun <T> invoke(channel: BroadcastChannel<T>) = BroadcastFlow(channel, channel.asFlow())
    }
}

inline fun <F, T, R> F.writeTo(flow: MutableStateFlow<R>, crossinline f: (T) -> R) where F : Flow<T>, F : CoroutineScope = apply {
    launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
        collect {
            flow.value = f(it)
        }
    }
}

inline fun <F, T, R> F.writeFrom(flow: Flow<R>, crossinline f: (R) -> T) where F : MutableStateFlow<T>, F : CoroutineScope = apply {
    launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
        flow.collect {
            this@apply.value = f(it)
        }
    }
}

/**
 * When a view exposes a [MutableStateFlow], by convention the view should not write to that flow,
 * that flow is for presenters to update the view's state.
 * When a view exposes a [ViewMutableStateFlow],  by convention the view has permissions to write to this flow as well,
 * to notify about changes to its' data.
 * Presenters should react to these changes.
 */
interface ViewMutableStateFlow<T> : MutableStateFlow<Value<T>> {
    val v: T get() = value.value

    var valueFromView: T
        get() = checkNotNull(value as? Value.FromView) { "Value is not from view!" }.value
        set(value) {
            this.value = value.fromView
        }

    var valueFromPresenter: T
        get() = checkNotNull(value as? Value.FromPresenter) { "Value is not from presenter!" }.value
        set(value) {
            this.value = value.fromPresenter
        }
}

sealed class Value<T> {
    abstract val value: T

    data class FromPresenter<T>(override val value: T) : Value<T>() {
        override fun asFromView() = value.fromView
    }

    data class FromView<T>(override val value: T) : Value<T>() {
        override fun asFromView() = this
    }

    abstract fun asFromView(): FromView<T>
}

val <T> T.fromView: Value.FromView<T> get() = Value.FromView(this)
val <T> T.fromPresenter: Value.FromPresenter<T> get() = Value.FromPresenter(this)

typealias AsyncValue<T> = StateFlow<AsyncValueState<T>>

sealed class AsyncValueState<T> {
    object Loading : AsyncValueState<Any>()
    data class Result<T>(val result: T) : AsyncValueState<T>()
    data class Error<T>(val e: Exception) : AsyncValueState<T>()

    companion object {
        @Suppress("UNCHECKED_CAST")
        inline fun <T> loading(): AsyncValueState<T> = Loading as AsyncValueState<T>
    }
}

/**
 * This class exists to work around a feature (or limitation) of StateFlow - value equality.
 * There are a lot of situations where views contain 2 fields - a field with a value, and a field whether that value is valid.
 * However, some presenters need to act on pairs of (value, valueIsValid), so they would zip the 2 flows together.
 * However, if the value was valid, the value was then changed but remained valid, the IsValid flow will not emit a new element,
 * which means the presenters zip will not fire.
 * So instead of an 'IsValid' field, we use a ValidationResult field, which is guaranteed to fire on each value change.
 */
data class ValidatedValue<T>(
    val value: T,
    val isValid: IsValid
)
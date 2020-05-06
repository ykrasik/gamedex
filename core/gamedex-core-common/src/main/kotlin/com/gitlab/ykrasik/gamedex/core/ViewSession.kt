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

package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.app.api.util.*
import com.gitlab.ykrasik.gamedex.core.util.ListEvent
import com.gitlab.ykrasik.gamedex.core.util.ListObservable
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.and
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach

/**
 * User: ykrasik
 * Date: 24/04/2018
 * Time: 08:17
 */
interface Presenter<in V> {
    fun present(view: V): ViewSession
}

abstract class ViewSession : CoroutineScope {
    override val coroutineContext = Dispatchers.Main.immediate + Job()
    private var _isShowing = false
    protected val isShowing get() = _isShowing

    suspend fun onShow() {
        check(!_isShowing) { "Presenter already showing: $this" }
        _isShowing = true
        onShown()
    }

    protected open suspend fun onShown() {}

    fun onHide() {
        check(_isShowing) { "Presenter wasn't showing: $this" }
        _isShowing = false
        onHidden()
    }

    protected open fun onHidden() {}

    fun destroy() {
        if (_isShowing) onHide()
        coroutineContext.cancel()
    }

    inline fun <T> ReceiveChannel<T>.forEach(crossinline f: suspend (T) -> Unit): Job = launch {
        consumeEach {
            try {
                f(it)
            } catch (e: Exception) {
                // FIXME: Handle this in a global error handler
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
            }
        }
    }

    inline fun <T> MultiReadChannel<T>.forEach(crossinline f: suspend (T) -> Unit) = subscribe().forEach(f)

    inline fun <T> StatefulMultiReadChannel<T>.forEach(crossinline f: suspend (T) -> Unit) =
        subscribe(coroutineContext, CoroutineStart.UNDISPATCHED) {
            try {
                f(it)
            } catch (e: Exception) {
                // FIXME: Handle this in a global error handler
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
            }
        }

    inline fun <T> StatefulMultiReadChannel<T>.onChange(crossinline f: suspend (T) -> Unit) =
        drop(1).distinctUntilChanged().subscribe(coroutineContext) {
            try {
                f(it)
            } catch (e: Exception) {
                // FIXME: Handle this in a global error handler
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
            }
        }

    inline fun <T> StatefulChannel<T>.bind(channel: StatefulMultiReadChannel<T>, crossinline also: (T) -> Unit = {}) =
        channel.distinctUntilChanged().subscribe(start = CoroutineStart.UNDISPATCHED) {
            value = it
            also(it)
        }

    fun <T> StatefulChannel<T>.bindBidirectional(channel: StatefulChannel<T>) {
        this.bind(channel)
        channel.bind(this.drop(1))
    }

    inline fun <T> StatefulChannel<IsValid>.bindIsValid(state: StatefulMultiReadChannel<T>, crossinline reason: (value: T) -> String?) {
        state.forEach { value ->
            val reasonMessage = reason(value)
            this.value = Try {
                check(reasonMessage == null) { reasonMessage!! }
            }
        }
    }

    inline fun StatefulChannel<IsValid>.enableWhenTrue(state: StatefulMultiReadChannel<Boolean>, crossinline reason: () -> String) =
        bindIsValid(state) {
            if (!it) reason() else null
        }

    inline fun StatefulChannel<IsValid>.disableWhenTrue(state: StatefulMultiReadChannel<Boolean>, crossinline reason: () -> String) =
        bindIsValid(state) {
            if (it) reason() else null
        }

    infix fun StatefulChannel<IsValid>.and(other: StatefulChannel<IsValid>) = value and other.value

    fun StatefulChannel<IsValid>.assert() {
        value.get()
    }

    fun <T> SettableList<T>.bind(list: ListObservable<T>) {
        this.setAll(list)
        list.changesChannel.forEach { event ->
            when (event) {
                is ListEvent.ItemAdded -> this += event.item
                is ListEvent.ItemsAdded -> this += event.items
                is ListEvent.ItemRemoved -> this.removeAt(event.index)
                is ListEvent.ItemsRemoved -> this.removeAll(event.items)
                is ListEvent.ItemSet -> this[event.index] = event.item
                is ListEvent.ItemsSet -> this.setAll(event.items)
                else -> Unit
            }
        }
    }

    fun <T> ViewMutableStatefulChannel<T>.debounce(millis: Long = 200): ReceiveChannel<T> =
        subscribe().debounce(millis, scope = this@ViewSession)

    inline fun <reified E : CoreEvent> EventBus.forEach(crossinline handler: suspend (E) -> Unit) =
        on<E>(coroutineContext) { event ->
            handler(event)
        }

    fun <V : Any> EventBus.requestHideView(view: V) = send(ViewEvent.RequestHide(view))
}

//inline fun <reified V> EventBus.onShowViewRequested(crossinline handler: suspend () -> Unit) =
//    on<ViewEvent.RequestShow>(Dispatchers.Main) {
//        if (it.viewClass == V::class) {
//            handler()
//        }
//    }

inline fun <reified V> EventBus.onHideViewRequested(crossinline handler: suspend (V) -> Unit) =
    on<ViewEvent.RequestHide>(Dispatchers.Main) {
        val view = it.view as? V
        if (view != null) {
            handler(view)
        }
    }
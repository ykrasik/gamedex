/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.app.api.State
import com.gitlab.ykrasik.gamedex.app.api.UserMutableState
import com.gitlab.ykrasik.gamedex.app.api.util.BroadcastReceiveChannel
import com.gitlab.ykrasik.gamedex.core.settings.SettingsRepository
import com.gitlab.ykrasik.gamedex.core.util.ListEvent
import com.gitlab.ykrasik.gamedex.core.util.ListObservable
import com.gitlab.ykrasik.gamedex.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlin.reflect.KProperty

/**
 * User: ykrasik
 * Date: 24/04/2018
 * Time: 08:17
 */
interface Presenter<in V> {
    fun present(view: V): ViewSession
}

abstract class ViewSession : CoroutineScope {
    override val coroutineContext = Dispatchers.Main + Job()
    private var _isShowing = false
    protected val isShowing get() = _isShowing

    suspend fun show() {
        check(!_isShowing) { "Presenter already showing: $this" }
        _isShowing = true
        onShow()
    }

    protected open suspend fun onShow() {}

    fun hide() {
        check(_isShowing) { "Presenter wasn't showing: $this" }
        _isShowing = false
        onHide()
    }

    protected open fun onHide() {}

    fun destroy() {
        if (_isShowing) hide()
        coroutineContext.cancel()
    }

    operator fun <T> UserMutableState<T>.getValue(thisRef: Any, property: KProperty<*>) = value
    operator fun <T> UserMutableState<T>.setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        this.value = value!!
    }

    operator fun <T> State<T>.getValue(thisRef: Any, property: KProperty<*>) = value
    operator fun <T> State<T>.setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        this.value = value!!
    }

    fun <T> UserMutableState<T>.forEach(f: suspend (T) -> Unit) = changes.forEach(f)

    operator fun <T> State<T>.timesAssign(value: T) {
        this.value = value
    }

    inline fun <T> State<T>.modify(f: Modifier<T>) {
        this.value = f(value)
    }

    fun State<IsValid>.and(other: State<IsValid>) = value.and(other.value)

    // TODO: This used to be inline, but the kotlin compiler was failing with internal errors. Make inline when solved.
    fun <T> ReceiveChannel<T>.forEach(f: suspend (T) -> Unit): Job = launch {
        consumeEach {
            try {
                f(it)
            } catch (e: Exception) {
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
            }
        }
    }

    inline fun <T> ReceiveChannel<T>.forEachImmediately(crossinline f: (T) -> Unit): Job {
        f(poll()!!)
        return forEach { f(it) }
    }

    // TODO: This used to be inline, but the kotlin compiler was failing with internal errors. Make inline when solved.
    fun <T> BroadcastReceiveChannel<T>.forEach(f: suspend (T) -> Unit) = subscribe().forEach(f)

    inline fun <T> BroadcastReceiveChannel<T>.forEachImmediately(crossinline f: (T) -> Unit) = subscribe().forEachImmediately(f)

    fun <T> ListObservable<T>.bind(list: MutableList<T>) {
        list.setAll(this)
        changesChannel.forEach { event ->
            when (event) {
                is ListEvent.ItemAdded -> list += event.item
                is ListEvent.ItemsAdded -> list += event.items
                is ListEvent.ItemRemoved -> list.removeAt(event.index)
                is ListEvent.ItemsRemoved -> list.removeAll(event.items)
                is ListEvent.ItemSet -> list[event.index] = event.item
                is ListEvent.ItemsSet -> list.setAll(event.items)
                else -> Unit
            }
        }
    }

    inline fun <S : SettingsRepository<Data>, T, Data : Any> S.bind(
        channelAccessor: S.() -> BroadcastReceiveChannel<T>,
        userMutableState: UserMutableState<T>,
        crossinline f: (Data).(T) -> Data
    ) {
        val channel = channelAccessor(this)
        channel.bind(userMutableState)
        userMutableState.changes.forEach { change ->
            this.modify { f(change) }
        }
    }

    fun <T> BroadcastReceiveChannel<T>.bind(state: State<T>) =
        forEachImmediately { state.value = it }

    inline fun <reified E : CoreEvent> EventBus.forEach(crossinline handler: suspend (E) -> Unit) =
        on<E>(Dispatchers.Main) { event ->
            handler(event)
        }

    inline fun <T> BroadcastReceiveChannel<T>.bindIsValid(state: State<IsValid>, crossinline reason: (value: T) -> String?) {
        forEachImmediately { value ->
            val reasonMessage = reason(value)
            state *= Try {
                check(reasonMessage == null) { reasonMessage!! }
            }
        }
    }

    inline fun BroadcastReceiveChannel<Boolean>.enableWhenTrue(state: State<IsValid>, crossinline reason: () -> String) =
        bindIsValid(state) {
            if (!it) reason() else null
        }

    inline fun BroadcastReceiveChannel<Boolean>.disableWhenTrue(state: State<IsValid>, crossinline reason: () -> String) =
        bindIsValid(state) {
            if (it) reason() else null
        }

    fun <V> EventBus.viewFinished(view: V) = send(ViewFinishedEvent(view))

    suspend inline fun <V> EventBus.awaitViewFinished(view: V) = awaitEvent<ViewFinishedEvent<V>> { it.view == view }
    inline fun <reified V> EventBus.onViewFinished(crossinline handler: (V) -> Unit) = forEach<ViewFinishedEvent<*>> {
        if (it.view is V) {
            handler(it.view)
        }
    }

    fun State<IsValid>.assert() {
        value.get()
    }
}
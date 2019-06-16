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

import com.gitlab.ykrasik.gamedex.app.api.util.*
import com.gitlab.ykrasik.gamedex.core.settings.SettingsRepository
import com.gitlab.ykrasik.gamedex.core.util.ListEvent
import com.gitlab.ykrasik.gamedex.core.util.ListObservable
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.Modifier
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.and
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

    infix fun State<IsValid>.and(other: State<IsValid>) = value and other.value

    // TODO: This used to be inline, but the kotlin compiler was failing with internal errors. Make inline when solved.
    fun <T> ReceiveChannel<T>.forEach(f: suspend (T) -> Unit): Job = launch {
        consumeEach {
            try {
                f(it)
            } catch (e: Exception) {
                // TODO: Delegate this to the view?
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
            }
        }
    }

    inline fun <T> ReceiveChannel<T>.forEachImmediately(crossinline f: (T) -> Unit): Job {
        f(poll()!!)
        return forEach { f(it) }
    }

    // TODO: This used to be inline, but the kotlin compiler was failing with internal errors. Make inline when solved.
    fun <T> MultiReceiveChannel<T>.forEach(f: suspend (T) -> Unit) = subscribe().forEach(f)

    inline fun <T> MultiReceiveChannel<T>.forEachImmediately(crossinline f: (T) -> Unit) = subscribe().forEachImmediately(f)

    fun <T> ListObservable<T>.bind(list: SettableList<T>) {
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
        channelAccessor: S.() -> MultiReceiveChannel<T>,
        userMutableState: UserMutableState<T>,
        crossinline f: (Data).(T) -> Data
    ) {
        val channel = channelAccessor(this)
        channel.bind(userMutableState)
        userMutableState.changes.forEach { change ->
            this.modify { f(change) }
        }
    }

    fun <T> MultiReceiveChannel<T>.bind(state: State<T>) =
        forEachImmediately { state.value = it }

    inline fun <reified E : CoreEvent> EventBus.forEach(crossinline handler: suspend (E) -> Unit) =
        on<E>(Dispatchers.Main) { event ->
            handler(event)
        }

    inline fun <T> MultiReceiveChannel<T>.bindIsValid(state: State<IsValid>, crossinline reason: (value: T) -> String?) {
        forEachImmediately { value ->
            val reasonMessage = reason(value)
            state *= Try {
                check(reasonMessage == null) { reasonMessage!! }
            }
        }
    }

    inline fun MultiReceiveChannel<Boolean>.enableWhenTrue(state: State<IsValid>, crossinline reason: () -> String) =
        bindIsValid(state) {
            if (!it) reason() else null
        }

    inline fun MultiReceiveChannel<Boolean>.disableWhenTrue(state: State<IsValid>, crossinline reason: () -> String) =
        bindIsValid(state) {
            if (it) reason() else null
        }

    fun <T> UserMutableState<T>.debounce(millis: Long = 200): ReceiveChannel<T> =
        changes.subscribe().debounce(millis, scope = this@ViewSession)

    fun <V : Any> EventBus.requestHideView(view: V) = send(ViewEvent.RequestHide(view))

    fun State<IsValid>.assert() {
        value.get()
    }
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
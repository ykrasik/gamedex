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

import com.gitlab.ykrasik.gamedex.app.api.util.SettableList
import com.gitlab.ykrasik.gamedex.app.api.util.Value
import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStateFlow
import com.gitlab.ykrasik.gamedex.app.api.util.fromPresenter
import com.gitlab.ykrasik.gamedex.core.util.FlowScope
import com.gitlab.ykrasik.gamedex.core.util.FlowWithDebugInfo
import com.gitlab.ykrasik.gamedex.core.util.ListEvent
import com.gitlab.ykrasik.gamedex.core.util.ListObservable
import com.gitlab.ykrasik.gamedex.core.view.ViewExceptionHandler
import com.gitlab.ykrasik.gamedex.util.IsValid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlin.reflect.KProperty

/**
 * User: ykrasik
 * Date: 24/04/2018
 * Time: 08:17
 */
interface Presenter<in V> {
    fun present(view: V): ViewSession
}

// TODO: A lot of these actions can actually happen on the Default context.
abstract class ViewSession : FlowScope(
    coroutineContext = SupervisorJob() + ViewExceptionHandler + Dispatchers.Main.immediate,
    baseDebugName = ""
) {
    override val baseDebugName: String get() = this.javaClass.enclosingClass!!.simpleName

    private var _isShowing = MutableStateFlow(false)
    protected val isShowing: StateFlow<Boolean> = _isShowing

    fun onShow() {
        check(!_isShowing.value) { "Session already showing: $this" }
        _isShowing.value = true
    }

    fun onHide() {
        check(_isShowing.value) { "Session wasn't showing: $this" }
        _isShowing.value = false
    }

    fun destroy() {
        if (_isShowing.value) onHide()
        coroutineContext.cancel()
    }

    inline fun <T> MutableStateFlow<IsValid>.bindIsValid(state: Flow<T>, crossinline reason: (value: T) -> String?) {
        state.forEach { value ->
            val reasonMessage = reason(value)
            this.value = IsValid {
                check(reasonMessage == null) { reasonMessage!! }
            }
        }
    }

    inline fun MutableStateFlow<IsValid>.enableWhenTrue(state: Flow<Boolean>, crossinline reason: () -> String) =
        bindIsValid(state) {
            if (!it) reason() else null
        }

    inline fun MutableStateFlow<IsValid>.disableWhenTrue(state: Flow<Boolean>, crossinline reason: () -> String) =
        bindIsValid(state) {
            if (it) reason() else null
        }

    fun StateFlow<IsValid>.assert() {
        value.getOrThrow()
    }

    fun <T> Flow<Value<T>>.allValues(): Flow<T> = map { it.value }
    fun <T> StateFlow<Value<T>>.onlyChangesFromView(): Flow<T> = drop(1).transform { if (it is Value.FromView) emit(it.value) }

//    fun <T> Flow<T>.asValuesFromPresenter(): Flow<Value.FromPresenter<T>> = map { it.fromPresenter }
    fun <T> FlowWithDebugInfo<T>.asValuesFromPresenter(): FlowWithDebugInfo<Value.FromPresenter<T>> = flow.map { it.fromPresenter }.withDebugInfoFrom(this)
    fun <T> Flow<Value<T>>.unwrap(): Flow<T> = map { it.value }

    operator fun <T> MutableStateFlow<T>.timesAssign(flow: ViewMutableStateFlow<T>) {
        timesAssign(flow.unwrap())
    }

    operator fun <T> ViewMutableStateFlow<T>.timesAssign(value: T) {
        valueFromPresenter = value
    }

    operator fun <T> ViewMutableStateFlow<T>.timesAssign(flow: FlowWithDebugInfo<T>) {
        timesAssign(flow.asValuesFromPresenter())
    }

    operator fun <T> ViewMutableStateFlow<T>.getValue(thisRef: Any, property: KProperty<*>): T = v
    operator fun <T> ViewMutableStateFlow<T>.setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this *= value
    }

    fun <T> ViewMutableStateFlow<T>.bindBidirectional(flow: MutableStateFlow<T>) {
        flow.forEach { valueFromPresenter = it }
        this.onlyChangesFromView().forEach { flow.value = it }
    }

    // FIXME: Only SettableList operations are limited by their context, the rest of the operations arent
    fun <T> SettableList<T>.bind(list: ListObservable<T>) {
        this.setAll(list)
        list.changes.forEach { event ->
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

    operator fun <T> SettableList<T>.timesAssign(list: ListObservable<T>) {
        bind(list)
    }

    operator fun <T> SettableList<T>.timesAssign(list: List<T>) {
        setAll(list)
    }

    operator fun <T> SettableList<T>.timesAssign(flow: Flow<Collection<T>>) {
        flow.forEach { if (this != it) setAll(it) }
    }

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

inline fun <reified V> EventBus.hideViewRequests() =
    flowOf<ViewEvent.RequestHide>().transform { (it.view as? V)?.let { emit(it) } }
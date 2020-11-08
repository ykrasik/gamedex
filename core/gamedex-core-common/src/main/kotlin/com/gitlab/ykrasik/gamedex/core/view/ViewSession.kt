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

package com.gitlab.ykrasik.gamedex.core.view

import com.gitlab.ykrasik.gamedex.app.api.util.SettableList
import com.gitlab.ykrasik.gamedex.app.api.util.Value
import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStateFlow
import com.gitlab.ykrasik.gamedex.app.api.util.fromPresenter
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.util.FlowScope
import com.gitlab.ykrasik.gamedex.core.util.FlowWithDebugInfo
import com.gitlab.ykrasik.gamedex.core.util.ListEvent
import com.gitlab.ykrasik.gamedex.core.util.ListObservable
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.setAll
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

/**
 * User: ykrasik
 * Date: 24/04/2018
 * Time: 08:17
 */
interface Presenter<in V> {
    fun present(view: V): ViewSession
}

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

    fun StateFlow<IsValid>.assert() {
        value.getOrThrow()
    }

    fun <T> Flow<Value<T>>.allValues(): Flow<T> = map { it.value }
    fun <T> StateFlow<Value<T>>.onlyChangesFromView(): Flow<T> = drop(1).transform { if (it is Value.FromView) emit(it.value) }

    operator fun <T> ViewMutableStateFlow<T>.divAssign(value: T) {
        valueFromPresenter = value
    }

    operator fun <T> ViewMutableStateFlow<T>.timesAssign(flow: FlowWithDebugInfo<T>) {
        bind(flow.map { it.fromPresenter }, flow.debugName, flow.traceValues)
    }

    operator fun <T> ViewMutableStateFlow<T>.getValue(thisRef: Any, property: KProperty<*>): T = v
    operator fun <T> ViewMutableStateFlow<T>.setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this /= value
    }

    fun <T> ViewMutableStateFlow<T>.bindBidirectional(flow: MutableStateFlow<T>, debugName: String, traceValues: Boolean = true) {
        flow.forEach("$debugName.fromPresenter", traceValues) { valueFromPresenter = it }
        this.onlyChangesFromView().forEach("$debugName.fromView", traceValues) { flow.value = it }
    }

    fun <T> KProperty0<ViewMutableStateFlow<T>>.bindBidirectional(flow: MutableStateFlow<T>, traceValues: Boolean = true) =
        get().bindBidirectional(flow, name, traceValues)

    // FIXME: Only SettableList operations are limited by their context, the rest of the operations arent
    fun <T> SettableList<T>.bind(list: ListObservable<T>, debugName: String, traceValues: Boolean = true) {
        val thisList = this
        thisList.setAll(list)
        list.changes.forEach(debugName, traceValues) { event ->
            when (event) {
                is ListEvent.ItemAdded -> thisList += event.item
                is ListEvent.ItemsAdded -> thisList += event.items
                is ListEvent.ItemRemoved -> thisList.removeAt(event.index)
                is ListEvent.ItemsRemoved -> thisList.removeAll(event.items)
                is ListEvent.ItemSet -> thisList[event.index] = event.item
                is ListEvent.ItemsSet -> thisList.setAll(event.items)
                else -> Unit
            }
        }
    }

    operator fun <T> KProperty0<SettableList<T>>.timesAssign(list: ListObservable<T>) {
        get().bind(list, name)
    }

    operator fun <T> MutableCollection<T>.divAssign(iterable: Iterable<T>) {
        setAll(iterable)
    }

    fun <V : Any> EventBus.requestHideView(view: V) = launch(start = CoroutineStart.UNDISPATCHED) {
        emit(ViewEvent.RequestHide(view))
    }
}
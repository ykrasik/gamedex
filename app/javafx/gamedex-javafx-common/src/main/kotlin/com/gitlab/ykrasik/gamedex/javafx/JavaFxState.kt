/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.app.api.util.Value
import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStateFlow
import com.gitlab.ykrasik.gamedex.app.api.util.fromView
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop

/**
 * User: ykrasik
 * Date: 02/12/2018
 * Time: 16:57
 */
val log = logger("")

class JavaFxViewMutableStateFlow<T, P : Property<T>>(
    private val flow: MutableStateFlow<Value<T>>,
    val property: P,
    debugName: String,
    traceValues: Boolean,
) : MutableStateFlow<Value<T>> by flow, ViewMutableStateFlow<T>, CoroutineScope {
    override val coroutineContext = Dispatchers.Main.immediate + CoroutineName(debugName)

    private var propertyChangedByPresenter = false

    init {
        property.typeSafeOnChange { value ->
            if (!propertyChangedByPresenter) {
                flow.value = value.fromView
            }
        }

        launch(start = CoroutineStart.UNDISPATCHED) {
            flow.drop(1).collect { value ->
                if (traceValues) log.trace("$value")
                propertyChangedByPresenter = value is Value.FromPresenter
                property.value = value.value
                propertyChangedByPresenter = false
            }
        }
    }

    inline fun perform(crossinline f: (T) -> Unit) = property.perform(f)
    inline fun onChange(crossinline f: (T) -> Unit) = property.typeSafeOnChange(f)
    fun onChangeFromPresenter(f: (T) -> Unit) = property.typeSafeOnChange {
        if (propertyChangedByPresenter) {
            f(it)
        }
    }
}

fun <T> Any.viewMutableStateFlow(initial: T, debugName: String, traceValues: Boolean = true) =
    viewMutableStateFlow(SimpleObjectProperty(initial), debugName, traceValues)

fun Any.viewMutableStateFlow(initial: Boolean, debugName: String, traceValues: Boolean = true) =
    viewMutableStateFlow(SimpleBooleanProperty(initial), debugName, traceValues)

fun Any.viewMutableStateFlow(initial: Double, debugName: String, traceValues: Boolean = true) =
    viewMutableStateFlow(SimpleObjectProperty(initial), debugName, traceValues)

fun Any.viewMutableStateFlow(initial: Int, debugName: String, traceValues: Boolean = true) =
    viewMutableStateFlow(SimpleObjectProperty(initial), debugName, traceValues)

fun Any.viewMutableStateFlow(initial: String, debugName: String, traceValues: Boolean = true) =
    viewMutableStateFlow(SimpleStringProperty(initial), debugName, traceValues)

fun <T, P : Property<T>> Any.viewMutableStateFlow(flow: JavaFxViewMutableStateFlow<T, P>, debugName: String, traceValues: Boolean = true) =
    viewMutableStateFlow(flow.property, debugName, traceValues)

fun <T, P : Property<T>> Any.viewMutableStateFlow(property: P, debugName: String, traceValues: Boolean = true) =
    JavaFxViewMutableStateFlow(
        MutableStateFlow(property.value.fromView),
        property,
        debugName = "${this.javaClass.simpleName}.$debugName",
        traceValues = traceValues
    )

open class JavaFxMutableStateFlow<T, P : Property<T>>(
    private val flow: MutableStateFlow<T>,
    val property: P,
    debugName: String,
    traceValues: Boolean,
) : MutableStateFlow<T> by flow, CoroutineScope {
    override val coroutineContext = Dispatchers.Main.immediate + CoroutineName(debugName)

    init {
        launch(start = CoroutineStart.UNDISPATCHED) {
            flow.collect { value ->
                if (traceValues) log.trace("$value")
                property.value = value
            }
        }
    }

    inline fun perform(crossinline f: (T) -> Unit) = property.perform(f)
    inline fun onChange(crossinline f: (T) -> Unit) = property.typeSafeOnChange(f)
    inline fun onInvalidated(crossinline f: (T) -> Unit) = property.onInvalidated(f)
}

fun <T> Any.mutableStateFlow(initial: T, debugName: String, traceValues: Boolean = true) =
    mutableStateFlow(SimpleObjectProperty(initial), debugName, traceValues)

fun Any.mutableStateFlow(initial: Boolean, debugName: String, traceValues: Boolean = true) =
    mutableStateFlow(SimpleBooleanProperty(initial), debugName, traceValues)

fun Any.mutableStateFlow(initial: Double, debugName: String, traceValues: Boolean = true) =
    mutableStateFlow(SimpleObjectProperty(initial), debugName, traceValues)

fun Any.mutableStateFlow(initial: Int, debugName: String, traceValues: Boolean = true) =
    mutableStateFlow(SimpleObjectProperty(initial), debugName, traceValues)

fun Any.mutableStateFlow(initial: String, debugName: String, traceValues: Boolean = true) =
    mutableStateFlow(SimpleStringProperty(initial), debugName, traceValues)

fun <T, P : Property<T>> Any.mutableStateFlow(flow: JavaFxMutableStateFlow<T, P>, debugName: String, traceValues: Boolean = true) =
    mutableStateFlow(flow.property, debugName, traceValues)

fun <T, P : Property<T>> Any.mutableStateFlow(property: P, debugName: String, traceValues: Boolean = true) =
    JavaFxMutableStateFlow(
        MutableStateFlow(property.value),
        property,
        debugName = "${this.javaClass.simpleName}.$debugName",
        traceValues = traceValues
    )

typealias JavaFxObjectMutableStateFlow<T> = JavaFxMutableStateFlow<T, SimpleObjectProperty<T>>

open class JavaFxListMutableStateFlow<T>(
    flow: MutableStateFlow<List<T>>,
    property: SimpleObjectProperty<List<T>>,
    debugName: String,
) : JavaFxMutableStateFlow<List<T>, SimpleObjectProperty<List<T>>>(flow, property, debugName, traceValues = false) {
    val list: ObservableList<T> = property.asObservableList()
}

fun <T> Any.mutableStateFlow(initial: List<T>, debugName: String) =
    JavaFxListMutableStateFlow(
        MutableStateFlow(initial),
        SimpleObjectProperty(initial),
        debugName = "${this.javaClass.simpleName}.$debugName"
    )

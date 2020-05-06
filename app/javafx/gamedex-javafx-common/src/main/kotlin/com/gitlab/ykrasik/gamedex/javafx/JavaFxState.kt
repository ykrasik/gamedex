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

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.app.api.util.ConflatedMultiChannel
import com.gitlab.ykrasik.gamedex.app.api.util.StatefulChannel
import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStatefulChannel
import com.gitlab.ykrasik.gamedex.app.api.util.conflatedChannel
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.coroutines.Dispatchers

/**
 * User: ykrasik
 * Date: 02/12/2018
 * Time: 16:57
 */

fun <T> viewMutableStatefulChannel(initial: T) = objectPropertyState(::JavaFxViewMutableStatefulChannel, initial)
fun viewMutableStatefulChannel(initial: Boolean) = booleanPropertyState(::JavaFxViewMutableStatefulChannel, initial)
fun viewMutableStatefulChannel(initial: Double) = doublePropertyState(::JavaFxViewMutableStatefulChannel, initial)
fun viewMutableStatefulChannel(initial: Int) = intPropertyState(::JavaFxViewMutableStatefulChannel, initial)
fun viewMutableStatefulChannel(initial: String) = stringPropertyState(::JavaFxViewMutableStatefulChannel, initial)
fun <T, P : Property<T>> viewMutableStatefulChannel(statefulChannel: JavaFxStatefulChannel<T, P>) = viewMutableStatefulChannel(statefulChannel.property)
fun <T, P : Property<T>> viewMutableStatefulChannel(property: P) = JavaFxViewMutableStatefulChannel(conflatedChannel(property.value), property)

fun <T> statefulChannel(initial: T) = objectPropertyState(::JavaFxStatefulChannel, initial)
fun statefulChannel(initial: Boolean) = booleanPropertyState(::JavaFxStatefulChannel, initial)
fun statefulChannel(initial: Double) = doublePropertyState(::JavaFxStatefulChannel, initial)
fun statefulChannel(initial: Int) = intPropertyState(::JavaFxStatefulChannel, initial)
fun statefulChannel(initial: String) = stringPropertyState(::JavaFxStatefulChannel, initial)

private inline fun <T, S> objectPropertyState(factory: (ConflatedMultiChannel<T>, SimpleObjectProperty<T>) -> S, initial: T): S =
    factory(conflatedChannel(initial), SimpleObjectProperty(initial))

private inline fun <S> booleanPropertyState(factory: (ConflatedMultiChannel<Boolean>, SimpleBooleanProperty) -> S, initial: Boolean): S =
    factory(conflatedChannel(initial), SimpleBooleanProperty(initial))

private inline fun <S> doublePropertyState(factory: (ConflatedMultiChannel<Double>, SimpleObjectProperty<Double>) -> S, initial: Double): S =
    factory(conflatedChannel(initial), SimpleObjectProperty(initial))

private inline fun <S> intPropertyState(factory: (ConflatedMultiChannel<Int>, SimpleObjectProperty<Int>) -> S, initial: Int): S =
    factory(conflatedChannel(initial), SimpleObjectProperty(initial))

private inline fun <S> stringPropertyState(factory: (ConflatedMultiChannel<String>, SimpleStringProperty) -> S, initial: String): S =
    factory(conflatedChannel(initial), SimpleStringProperty(initial))

typealias JavaFxObjectStatefulChannel<T> = JavaFxStatefulChannel<T, SimpleObjectProperty<T>>

class JavaFxViewMutableStatefulChannel<T, P : Property<T>>(private val channel: ConflatedMultiChannel<T>, val property: P) : StatefulChannel<T> by channel, ViewMutableStatefulChannel<T> {
    init {
        var reportNextChangeToChannel = true

        property.onInvalidated { value ->
            if (reportNextChangeToChannel) {
                channel.value = value
            }
        }

        channel.subscribe(Dispatchers.Main.immediate) { value ->
            reportNextChangeToChannel = false
            property.value = value
            reportNextChangeToChannel = true
        }
    }

    inline fun perform(crossinline f: (T) -> Unit) = property.perform(f)
    inline fun onChange(crossinline f: (T) -> Unit) = property.typeSafeOnChange(f)
    inline fun onInvalidated(crossinline f: (T) -> Unit) = property.onInvalidated(f)

    override fun toString() = property.value.toString()
}

class JavaFxStatefulChannel<T, P : Property<T>>(private val channel: ConflatedMultiChannel<T>, val property: P) : StatefulChannel<T> by channel {
    init {
        channel.subscribe(Dispatchers.Main.immediate) { value ->
            property.value = value
        }
    }

    inline fun perform(crossinline f: (T) -> Unit) = property.perform(f)
    inline fun onChange(crossinline f: (T) -> Unit) = property.typeSafeOnChange(f)
    inline fun onInvalidated(crossinline f: (T) -> Unit) = property.onInvalidated(f)
}
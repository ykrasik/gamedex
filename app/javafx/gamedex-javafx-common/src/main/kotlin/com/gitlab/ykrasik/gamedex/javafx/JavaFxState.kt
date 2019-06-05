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

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.app.api.State
import com.gitlab.ykrasik.gamedex.app.api.UserMutableState
import com.gitlab.ykrasik.gamedex.app.api.util.MultiChannel
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 02/12/2018
 * Time: 16:57
 */

fun <T> userMutableState(initial: T) = objectPropertyState(::JavaFxUserMutableState, initial)
fun userMutableState(initial: Boolean) = booleanPropertyState(::JavaFxUserMutableState, initial)
fun userMutableState(initial: Double) = doublePropertyState(::JavaFxUserMutableState, initial)
fun userMutableState(initial: Int) = intPropertyState(::JavaFxUserMutableState, initial)
fun userMutableState(initial: String) = stringPropertyState(::JavaFxUserMutableState, initial)
fun <T, P : Property<T>> userMutableState(state: JavaFxState<T, P>) = JavaFxUserMutableState(state.property)
fun <T, P : Property<T>> userMutableState(property: P) = JavaFxUserMutableState(property)

fun <T> state(initial: T) = objectPropertyState(::JavaFxState, initial)
fun state(initial: Boolean) = booleanPropertyState(::JavaFxState, initial)
fun state(initial: Double) = doublePropertyState(::JavaFxState, initial)
fun state(initial: Int) = intPropertyState(::JavaFxState, initial)
fun state(initial: String) = stringPropertyState(::JavaFxState, initial)

private inline fun <T, S> objectPropertyState(factory: (SimpleObjectProperty<T>) -> S, initial: T): S = factory(SimpleObjectProperty(initial))
private inline fun <S> booleanPropertyState(factory: (SimpleBooleanProperty) -> S, initial: Boolean): S = factory(SimpleBooleanProperty(initial))
private inline fun <S> doublePropertyState(factory: (SimpleObjectProperty<Double>) -> S, initial: Double): S = factory(SimpleObjectProperty(initial))
private inline fun <S> intPropertyState(factory: (SimpleObjectProperty<Int>) -> S, initial: Int): S = factory(SimpleObjectProperty(initial))
private inline fun <S> stringPropertyState(factory: (SimpleStringProperty) -> S, initial: String): S = factory(SimpleStringProperty(initial))

typealias JavaFxObjectState<T> = JavaFxState<T, SimpleObjectProperty<T>>

class JavaFxUserMutableState<T, P : Property<T>>(val property: P) : UserMutableState<T> {
    private var ignoreNextChange = false

    init {
        onChange {
            if (!ignoreNextChange) {
                changes.offer(it)
            }
        }
    }

    // Called from outside the view to let the view know about changes. Does not report a change event.
    override var value: T
        get() = property.value
        set(value) {
            ignoreNextChange = true
            property.value = value
            ignoreNextChange = false
        }

    // To be used when the view needs to update the value programmatically and report a change event.
    var valueFromView: T by property

    override val changes = MultiChannel<T>()

    inline fun onChange(crossinline f: (T) -> Unit) = property.typeSafeOnChange(f)
    inline fun onInvalidated(crossinline f: (T) -> Unit) = property.onInvalidated(f)

    override fun toString() = value.toString()
}

class JavaFxState<T, P : Property<T>>(val property: P) : State<T> {
    override var value: T by property

    inline fun onChange(crossinline f: (T) -> Unit) = property.typeSafeOnChange(f)
    inline fun onInvalidated(crossinline f: (T) -> Unit) = property.onInvalidated(f)

    override fun toString() = value.toString()
}
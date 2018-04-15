/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import tornadofx.observable

/**
 * User: ykrasik
 * Date: 02/04/2018
 * Time: 17:13
 */
fun <T> ReceiveChannel<T>.toObservableValue(defaultInitial: T): ObservableValue<T> =
    toObservableValue { poll() ?: defaultInitial }

fun <T> ReceiveChannel<T>.toObservableValue(): ObservableValue<T> =
    toObservableValue { poll()!! }

private inline fun <T> ReceiveChannel<T>.toObservableValue(defaultProvider: () -> T): ObservableValue<T> {
    val p = SimpleObjectProperty<T>(defaultProvider())
    javaFx {
        consumeEach {
            p.value = it
        }
    }
    return p
}

fun <T> ReceiveChannel<T>.toObservableList(): ObservableList<T> {
    val list = mutableListOf<T>().observable()
    javaFx {
        consumeEach {
            list += it
        }
    }
    return list
}
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

import com.gitlab.ykrasik.gamedex.core.api.util.BroadcastReceiveChannel
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch

/**
 * User: ykrasik
 * Date: 02/04/2018
 * Time: 17:13
 */
fun <T> ReceiveChannel<T>.toObservableValue(initial: T): ObservableValue<T> {
    // FIXME: Find a better way of doing this.
    val p = SimpleObjectProperty<T>(initial)
    launch(JavaFx) {
        for (value in this@toObservableValue) {
            p.value = value
        }
    }
    return p
}

fun <T> BroadcastReceiveChannel<T>.subscribe(f: suspend (T) -> Unit) {
    launch(JavaFx) {
        subscribe().consumeEach { f(it) }
    }
}
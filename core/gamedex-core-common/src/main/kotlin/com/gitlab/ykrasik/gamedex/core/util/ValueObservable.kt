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

package com.gitlab.ykrasik.gamedex.core.util

import com.gitlab.ykrasik.gamedex.app.api.util.MultiChannel
import com.gitlab.ykrasik.gamedex.app.api.util.MultiReceiveChannel
import com.gitlab.ykrasik.gamedex.util.Extractor
import com.gitlab.ykrasik.gamedex.util.Modifier
import kotlinx.coroutines.CoroutineStart
import java.io.Closeable

/**
 * User: ykrasik
 * Date: 02/10/2019
 * Time: 21:38
 */
interface ValueObservable<T> : Closeable {
    var value: T
    val valueChannel: MultiReceiveChannel<T>
}

fun <T> ValueObservable<T>.modify(f: Modifier<T>) {
    value = f(value)
}

fun <T> ValueObservable<T>.perform(f: suspend (T) -> Unit) =
    valueChannel.subscribe(start = CoroutineStart.UNDISPATCHED) { f(it) }

fun <T> ValueObservable<T>.onChange(f: suspend (T) -> Unit) =
    valueChannel.drop(1).subscribe(f = f)

fun <T, R> ValueObservable<T>.channel(extractor: Extractor<T, R>): MultiReceiveChannel<R> {
    val channel = MultiChannel.conflated<R>()
    valueChannel.subscribe(start = CoroutineStart.UNDISPATCHED) {
        channel.send(extractor(it))
    }
    return channel.distinctUntilChanged()
}

class ValueObservableImpl<T> : ValueObservable<T> {
    private val _valueChannel = MultiChannel.conflated<T>()
    override val valueChannel = _valueChannel.distinctUntilChanged()
    override var value by _valueChannel

    override fun close() {
        _valueChannel.close()
        valueChannel.close()
    }

    override fun toString() = value.toString()
}
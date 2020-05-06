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

package com.gitlab.ykrasik.gamedex.app.api.util

import com.gitlab.ykrasik.gamedex.util.Modifier
import kotlinx.coroutines.channels.ReceiveChannel
import kotlin.reflect.KProperty

/**
 * User: ykrasik
 * Date: 02/12/2018
 * Time: 16:44
 */

/**
 * A conflated channel that always has a value.
 * By convention, this channel is read-only for any view that receives it,
 * i.e. the view should not update its' value or send messages to this channel, only read them.
 */
interface StatefulChannel<T> : StatefulMultiReadChannel<T>, MultiWriteChannel<T> {
    override var value: T

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        offer(value)
    }

    operator fun timesAssign(element: T) {
        offer(element)
    }

    fun modify(f: Modifier<T>) {
        value = f(value)
    }

    override fun <R> map(transform: suspend (T) -> R): StatefulChannel<R>
    override fun <R> flatMap(transform: suspend (T) -> ReceiveChannel<R>): StatefulChannel<R>
    override fun <T2, R> combineLatest(channel: MultiReadChannel<T2>, f: suspend (T, T2) -> R): StatefulChannel<R>
    override fun <T2, T3, R> combineLatest(channel2: MultiReadChannel<T2>, channel3: MultiReadChannel<T3>, f: suspend (T, T2, T3) -> R): StatefulChannel<R>
    override fun <T2, T3, T4, R> combineLatest(
        channel2: MultiReadChannel<T2>,
        channel3: MultiReadChannel<T3>,
        channel4: MultiReadChannel<T4>,
        f: suspend (T, T2, T3, T4) -> R
    ): StatefulChannel<R>

    override fun <T2, T3, T4, T5, R> combineLatest(
        channel2: MultiReadChannel<T2>,
        channel3: MultiReadChannel<T3>,
        channel4: MultiReadChannel<T4>,
        channel5: MultiReadChannel<T5>,
        f: suspend (T, T2, T3, T4, T5) -> R
    ): StatefulChannel<R>

    override fun filter(filter: suspend (T) -> Boolean): StatefulChannel<T>
    override fun distinctUntilChanged(equals: (T, T) -> Boolean): StatefulChannel<T>
    override fun drop(amount: Int): StatefulChannel<T>
}

/**
 * Same as [StatefulChannel], except by convention the view has permissions to write to this channel as well,
 * to notify about changes to its' data.
 * Presenters should react to these changes.
 */
interface ViewMutableStatefulChannel<T> : StatefulChannel<T>
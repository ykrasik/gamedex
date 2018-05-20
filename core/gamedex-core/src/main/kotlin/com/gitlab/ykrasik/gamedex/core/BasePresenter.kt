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

package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.app.api.util.*
import com.gitlab.ykrasik.gamedex.core.api.util.uiThreadDispatcher
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.launch

/**
 * User: ykrasik
 * Date: 24/04/2018
 * Time: 08:17
 */
fun launchOnUi(f: suspend () -> Unit) {
    launch(uiThreadDispatcher) {
        f()
    }
}

fun <T> ListObservable<T>.bindTo(list: MutableList<T>): SubscriptionReceiveChannel<ListChangeEvent<T>> {
    list.clear()
    list.addAll(this)
    return reportChangesTo(list)
}

fun <T> ListObservable<T>.reportChangesTo(list: MutableList<T>): SubscriptionReceiveChannel<ListChangeEvent<T>> =
    changesChannel.subscribe(uiThreadDispatcher) { event ->
        when (event) {
            is ListItemAddedEvent -> list += event.item
            is ListItemsAddedEvent -> list += event.items
            is ListItemRemovedEvent -> list.removeAt(event.index)
            is ListItemsRemovedEvent -> list.removeAll(event.items)
            is ListItemSetEvent -> list[event.index] = event.item
            is ListItemsSetEvent -> {
                list.clear()
                list.addAll(event.items)
            }
        }
    }

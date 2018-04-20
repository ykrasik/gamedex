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

package com.gitlab.ykrasik.gamedex.core.api

import com.gitlab.ykrasik.gamedex.core.api.util.uiThreadDispatcher
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import java.io.Closeable
import kotlin.coroutines.experimental.CoroutineContext

/**
 * User: ykrasik
 * Date: 15/04/2018
 * Time: 11:44
 */
abstract class ViewModel<Event, Action> : Closeable {
    private val onClose = mutableListOf<() -> Unit>()

    val events: SendChannel<Event> = Channel()
    val actions: ReceiveChannel<Action> = Channel()

    init {
        onClose {
            events.close()
            actions.cancel()
        }
    }

    fun onClose(f: () -> Unit) {
        this.onClose += f
    }

    override fun close() = onClose.forEach { it() }

    fun consumeActions(context: CoroutineContext = uiThreadDispatcher, f: suspend (action: Action) -> Unit) {
        launch(context) {
            actions.consumeEach { action ->
                f(action)
            }
        }
    }
}
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
import com.gitlab.ykrasik.gamedex.core.settings.SettingsRepository
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.reflect.KMutableProperty0

/**
 * User: ykrasik
 * Date: 24/04/2018
 * Time: 08:17
 */
abstract class Presenter {
    protected val jobs = mutableListOf<Job>()

    private var _showing = false
    protected val showing get() = _showing

    fun show() {
        check(!_showing) { "Presenter already showing: $this" }
        _showing = true
        onShow()
    }

    protected open fun onShow() {}

    fun hide() {
        check(_showing) { "Presenter wasn't showing: $this" }
        _showing = false
        onHide()
    }

    protected open fun onHide() {}

    fun destroy() {
        if (_showing) hide()
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    protected inline fun managed(f: () -> Job) {
        jobs += f()
    }

    protected inline fun <T> ReceiveChannel<T>.actionOnUi(crossinline f: suspend (T) -> Unit) = actionOn(uiThreadDispatcher, f)

    protected inline fun <T> ReceiveChannel<T>.actionOn(context: CoroutineContext, crossinline f: suspend (T) -> Unit) = managed {
        launch(context) {
            consumeEach {
                try {
                    f(it)
                } catch (e: Exception) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
                }
            }
        }
    }

    protected inline fun <T> ReceiveChannel<T>.subscribeOnUi(crossinline f: (T) -> Unit) = actionOnUi {
        f(it)
    }

    protected inline fun <T> BroadcastReceiveChannel<T>.actionOnUi(crossinline f: suspend (T) -> Unit) =
        subscribe().actionOnUi(f)

    protected inline fun <T> BroadcastReceiveChannel<T>.subscribeOnUi(crossinline f: (T) -> Unit) =
        subscribe().subscribeOnUi(f)

    protected fun <T> ListObservable<T>.bindTo(list: MutableList<T>) {
        list.clear()
        list.addAll(this)
        changesChannel.subscribeOnUi { event ->
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
    }

    protected inline fun <S : SettingsRepository<Data>, T, Data : Any> S.bind(channelAccessor: S.() -> BroadcastReceiveChannel<T>,
                                                                              viewProperty: KMutableProperty0<T>,
                                                                              changesChannel: ReceiveChannel<T>,
                                                                              context: CoroutineContext = CommonPool,
                                                                              crossinline f: (Data).(T) -> Data) {
        val channel = channelAccessor(this)
        channel.reportChangesTo(viewProperty)
        changesChannel.actionOn(context) { change ->
            this.modify { f(change) }
        }
    }

    protected fun <T> BroadcastReceiveChannel<T>.reportChangesTo(viewProperty: KMutableProperty0<T>,
                                                                 context: CoroutineContext = uiThreadDispatcher) {
        viewProperty.set(peek()!!)
        subscribe().actionOn(context) { viewProperty.set(it) }
    }
}

interface PresenterFactory<in V> {
    fun present(view: V): Presenter
}
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

package com.gitlab.ykrasik.gamedex.javafx.view

import com.gitlab.ykrasik.gamedex.app.api.ViewCanDisplayError
import com.gitlab.ykrasik.gamedex.app.api.ViewRegistry
import com.gitlab.ykrasik.gamedex.app.api.util.MultiChannel
import com.gitlab.ykrasik.gamedex.javafx.NotificationType
import com.gitlab.ykrasik.gamedex.javafx.notification
import com.gitlab.ykrasik.gamedex.javafx.typeSafeOnChange
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ButtonBase
import javafx.scene.layout.HBox
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.View
import tornadofx.action
import tornadofx.whenDocked
import tornadofx.whenUndocked

/**
 * User: ykrasik
 * Date: 21/04/2018
 * Time: 07:13
 */
abstract class PresentableView(title: String? = null, icon: Node? = null) : View(title, icon), ViewCanDisplayError {
    private val viewRegistry: ViewRegistry by di()

    // All tabs (which we use as screens) will have 'onDock' called even though they're not actually showing.
    // This is just how TornadoFx works.
    protected var skipFirstDock = false
    protected var skipFirstUndock = false

    init {
        whenDocked {
            if (!skipFirstDock) {
                viewRegistry.onShow(this)
            }
            skipFirstDock = false
        }
        whenUndocked {
            if (!skipFirstUndock) {
                viewRegistry.onHide(this)
            }
            skipFirstUndock = false
        }
    }

    override fun onError(message: String, title: String?, e: Exception?) =
        notification(NotificationType.Error, text = message, title = title)

    protected fun register() = viewRegistry.onCreate(this)

    fun <E> MultiChannel<E>.event(e: E) = offer(e)

    fun <T> ObservableValue<T>.bindChanges(channel: MultiChannel<T>): ChangeListener<T> = bindChanges(channel) { it }
    inline fun <T, R> ObservableValue<T>.bindChanges(channel: MultiChannel<R>, crossinline factory: (T) -> R): ChangeListener<T> =
        typeSafeOnChange { channel.event(factory(it)) }

    fun ButtonBase.action(channel: MultiChannel<Unit>) = action(channel) { }
    inline fun <T> ButtonBase.action(channel: MultiChannel<T>, crossinline f: () -> T) = apply {
        action { channel.offer(f()) }
    }
}

abstract class PresentableTabView(title: String? = null, icon: Node? = null) : PresentableView(title, icon) {
    init {
        // All tabs (which we use as screens) will have 'onDock' called even though they're not actually showing.
        // This is just how TornadoFx works.
        skipFirstDock = true
    }
}

abstract class PresentableScreen(title: String = "", icon: FontIcon? = null) : PresentableTabView(title, icon) {
    open val customNavigationButton: Button? = null

    abstract fun HBox.buildToolbar()
}
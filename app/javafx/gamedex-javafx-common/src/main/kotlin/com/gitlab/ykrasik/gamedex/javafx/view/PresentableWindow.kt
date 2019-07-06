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

package com.gitlab.ykrasik.gamedex.javafx.view

import com.gitlab.ykrasik.gamedex.app.api.ConfirmationView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.control.defaultHbox
import com.gitlab.ykrasik.gamedex.javafx.control.enableWhen
import com.gitlab.ykrasik.gamedex.javafx.control.prettyToolbar
import com.gitlab.ykrasik.gamedex.javafx.control.verticalGap
import com.gitlab.ykrasik.gamedex.javafx.state
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.acceptButton
import com.gitlab.ykrasik.gamedex.javafx.theme.cancelButton
import com.gitlab.ykrasik.gamedex.javafx.theme.header
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import tornadofx.*

/**
 * User: ykrasik
 * Date: 18/12/2018
 * Time: 20:50
 */
abstract class ConfirmationWindow(title: String? = null, icon: Node? = null) : PresentableView(title, icon), ConfirmationView {
    override val canAccept = state(IsValid.valid)
    override val acceptActions = channel<Unit>()
    override val cancelActions = channel<Unit>()

    protected val acceptButton = acceptButton {
        enableWhen(canAccept)
        action(acceptActions)
    }
    protected val cancelButton = cancelButton { action(cancelActions) }

    init {
        var installedHandler = false
        whenDocked {
            if (installedHandler) return@whenDocked
            installedHandler = true
            root.addEventHandler(KeyEvent.KEY_PRESSED) { e ->
                if (e.code == KeyCode.ENTER) {
                    acceptButton.fire()
                    e.consume()
                }
            }
            root.addEventFilter(KeyEvent.KEY_PRESSED) { e ->
                if (e.code == KeyCode.ESCAPE) {
                    cancelButton.fire()
                    e.consume()
                }
            }
        }
    }

    protected fun confirmationToolbar(toolbarOp: HBox.() -> Unit = { centeredWindowHeader() }) = prettyToolbar {
        children += cancelButton
        toolbarOp()
        children += acceptButton
    }

    protected fun buildAreYouSure(minHeight: Number? = 150, minWidth: Number? = 400, op: (VBox.() -> Unit)? = null) = borderpane {
        if (minHeight != null) this.minHeight = minHeight.toDouble()
        if (minWidth != null) this.minWidth = minWidth.toDouble()
        top = confirmationToolbar { spacer() }
        center = vbox(spacing = 10) {
            paddingAll = 20
            defaultHbox {
                header(titleProperty)
                spacer()
                hbox {
                    paddingRight = 20
                    paddingLeft = 40
                    add(icon ?: Icons.warning)
                }
            }
            if (op != null) {
                verticalGap()
                vbox(spacing = 10) {
                    op()
                }
            }
        }
    }

    fun EventTarget.windowHeader() = header(titleProperty, iconProperty)
    inline fun EventTarget.centeredWindowHeader(crossinline f: StackPane.() -> Unit = {}) = stackpane {
        hgrow = Priority.ALWAYS
        windowHeader()
        f()
    }
}
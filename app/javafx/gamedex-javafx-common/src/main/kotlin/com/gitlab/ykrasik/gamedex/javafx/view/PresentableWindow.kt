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
import com.gitlab.ykrasik.gamedex.app.api.task.ViewWithRunningTask
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.state
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.layout.*
import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 18/12/2018
 * Time: 20:50
 */
/**
 * A window that becomes invisible while a task is running, so the user may see task progress (which is displayed over the main window).
 */
abstract class PresentableWindow(title: String? = null, icon: Node? = null) : PresentableView(title, icon), ViewWithRunningTask {
    final override val isRunningTask = state(false)

    protected val windowOpacityProperty = SimpleObjectProperty(1.0)
    protected var windowOpacity by windowOpacityProperty

    init {
        whenDockedOnce {
            val stage = currentStage!!
            stage.draggableResizable()
            stage.opacityProperty().cleanBind(windowOpacityProperty.doubleBinding { it!! })
            stage.scene.fill = Color.TRANSPARENT

            val root = this.root
            if (root is Region) {
                stage.minWidthProperty().bind(root.minWidthProperty().doubleBinding { if (it!!.toDouble() >= 0.0) it.toDouble() else 0.0 })
                stage.minHeightProperty().bind(root.minHeightProperty().doubleBinding { if (it!!.toDouble() >= 0.0) it.toDouble() else 0.0 })
                stage.sizeToScene()
            }

            root.addClass(CommonStyle.gamedexWindow)
            val newRoot = StackPane().apply {
                add(root)
                rectangle {
                    x = 1.0
                    y = 1.0
                    arcWidth = 8.0
                    arcHeight = 8.0
                    heightProperty().bind(this@apply.heightProperty().subtract(1))
                    widthProperty().bind(this@apply.widthProperty().subtract(1))
                    fill = Color.TRANSPARENT
                    stroke = Color.GRAY
                    isMouseTransparent = true
                }
                clipRectangle {
                    arcHeight = 10.0
                    arcWidth = 10.0
                    heightProperty().bind(stage.heightProperty())
                    widthProperty().bind(stage.widthProperty())
                }
            }
            stage.scene.root = newRoot
        }

        var prevOpacity = windowOpacity
        isRunningTask.property.onChange { isRunningTask ->
            // Make the window invisible while running any task.
            if (isRunningTask) {
                prevOpacity = windowOpacity
                windowOpacity = 0.0
            } else {
                windowOpacity = prevOpacity
            }
        }
    }

    fun resizeToContent() = currentStage!!.sizeToScene()
}

abstract class ConfirmationWindow(title: String? = null, icon: Node? = null) : PresentableWindow(title, icon), ConfirmationView {
    override val canAccept = state(IsValid.valid)
    override val acceptActions = channel<Unit>()
    override val cancelActions = channel<Unit>()

    protected inline fun confirmationToolbar(crossinline toolbarOp: HBox.() -> Unit = { centeredWindowHeader() }) = customToolbar {
        cancelButton { action(cancelActions) }
        toolbarOp()
        acceptButton {
            enableWhen(canAccept)
            action(acceptActions)
        }
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
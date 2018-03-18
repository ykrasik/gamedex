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

package com.gitlab.ykrasik.gamedex.ui.view.dialog

import com.gitlab.ykrasik.gamedex.ui.imageview
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.theme.acceptButton
import com.gitlab.ykrasik.gamedex.ui.theme.cancelButton
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.stage.StageStyle
import tornadofx.*

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 19:45
 */
fun areYouSureDialog(text: String = "Are You Sure?", op: (VBox.() -> Unit)? = null): Boolean = object : Fragment(text) {
    private var accept = false

    override val root = borderpane {
        minWidth = 400.0
        minHeight = 100.0
        top {
            toolbar {
                acceptButton {
                    setOnAction { close(accept = true) }
                }
                spacer()
                cancelButton {
                    isCancelButton = true
                    setOnAction { close(accept = false) }
                }
            }
        }
        center {
            vbox(spacing = 10.0) {
                hbox {
                    paddingAll = 20.0
                    alignment = Pos.CENTER_LEFT
                    label(text)
                    spacer()
                    imageview(Theme.Images.warning)
                }
                if (op != null) {
                    separator()
                    vbox(spacing = 10.0) {
                        paddingAll = 20.0
                        paddingRight = 30.0
                        op(this)
                    }
                }
            }
        }
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    fun show(): Boolean {
        openModal(block = true, stageStyle = StageStyle.UNIFIED)
        return accept
    }
}.show()

fun infoAlert(title: String = "", op: (VBox.() -> Unit)? = null) = alert(title, Theme.Images.information, op)
fun errorAlert(title: String = "Error", op: (VBox.() -> Unit)? = null) = alert(title, Theme.Images.error, op)
private fun alert(title: String, image: Image, op: (VBox.() -> Unit)? = null) = object : Fragment(title) {
    override val root = borderpane {
        minWidth = 400.0
        minHeight = 100.0
        top {
            toolbar {
                acceptButton { setOnAction { close() } }
            }
        }
        center {
            vbox(spacing = 10.0) {
                hbox {
                    paddingAll = 20.0
                    alignment = Pos.CENTER_LEFT
                    label(title) {
                        font = Font.font(16.0)
                    }
                    spacer()
                    imageview(image)
                }
                if (op != null) {
                    separator()
                    vbox(spacing = 10.0) {
                        paddingAll = 20.0
                        paddingRight = 30.0
                        op(this)
                    }
                }
            }
        }
    }

    fun show() {
        openModal(block = true, stageStyle = StageStyle.UNIFIED)
    }
}.show()
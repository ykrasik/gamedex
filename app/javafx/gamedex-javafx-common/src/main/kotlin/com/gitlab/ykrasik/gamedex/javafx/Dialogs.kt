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

import com.gitlab.ykrasik.gamedex.javafx.control.defaultHbox
import javafx.scene.layout.VBox
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
                cancelButton { action { close(accept = false) } }
                spacer()
                acceptButton { action { close(accept = true) } }
            }
        }
        center {
            vbox(spacing = 10.0) {
                defaultHbox {
                    paddingAll = 20.0
                    label(text)
                    spacer()
                    children += Icons.warning
                }
                if (op != null) {
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
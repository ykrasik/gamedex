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

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import javafx.scene.Node
import javafx.scene.layout.VBox
import javafx.stage.StageStyle
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.selects.select

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 19:45
 */
fun areYouSureDialog(
    text: String = "Are You Sure?",
    icon: Node? = null,
    op: (VBox.() -> Unit)? = null
): Boolean = AreYouSureDialog(text, icon, op).show()

class AreYouSureDialog(title: String, icon: Node?, op: (VBox.() -> Unit)?) : ConfirmationWindow(title, icon), CoroutineScope {
    override val coroutineContext = Dispatchers.JavaFx + Job()

    private var accept = false

    override val root = buildAreYouSure(op = op)

    init {
        skipFirstDock = true
        skipFirstUndock = true

        launch {
            val accept = select<Boolean> {
                acceptActions.onReceive { true }
                cancelActions.onReceive { false }
            }
            coroutineContext.cancel()
            acceptActions.close()
            cancelActions.close()
            close(accept)
        }
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    fun show(): Boolean {
        openModal(StageStyle.TRANSPARENT, block = true)
        return accept
    }

    override fun hide() = close()
}
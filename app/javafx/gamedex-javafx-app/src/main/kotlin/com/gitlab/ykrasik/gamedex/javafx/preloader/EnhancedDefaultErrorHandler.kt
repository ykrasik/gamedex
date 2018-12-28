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

package com.gitlab.ykrasik.gamedex.javafx.preloader

import com.gitlab.ykrasik.gamedex.core.task.ExpectedException
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.VBox
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import tornadofx.add
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

/**
 * A copy of [tornadofx.DefaultErrorHandler] that ignores [CancellationException].
 * It exists because CancellationExceptions were being logged to the logger as errors.
 */
class EnhancedDefaultErrorHandler : Thread.UncaughtExceptionHandler {
    private val log = logger("ErrorHandler")

    override fun uncaughtException(t: Thread, error: Throwable) {
        if (error is ExpectedException) return
        if (error is CancellationException && error !is ClosedSendChannelException) return

        log.error("Uncaught error", error)

        if (isCycle(error)) {
            log.info("Detected cycle handling error, aborting.", error)
        } else {
            GlobalScope.launch(Dispatchers.JavaFx) {
                showErrorDialog(error)
            }
        }
    }

    private fun isCycle(error: Throwable) = error.stackTrace.any {
        it.className.startsWith("${javaClass.name}\$uncaughtException$")
    }

    private fun showErrorDialog(error: Throwable) {
        val cause = Label(if (error.cause != null) error.cause?.message else "").apply {
            style = "-fx-font-weight: bold"
        }

        val textarea = TextArea().apply {
            prefRowCount = 20
            prefColumnCount = 50
            text = stringFromError(error)
        }

        Alert(Alert.AlertType.ERROR).apply {
            title = error.message ?: "An error occured"
            isResizable = true
            headerText = if (error.stackTrace?.isEmpty() != false) "Error" else "Error in " + error.stackTrace[0].toString()
            dialogPane.content = VBox().apply {
                add(cause)
                add(textarea)
            }

            showAndWait()
        }
    }

    private fun stringFromError(e: Throwable): String {
        val out = ByteArrayOutputStream()
        val writer = PrintWriter(out)
        e.printStackTrace(writer)
        writer.close()
        return out.toString()
    }
}

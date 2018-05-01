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

import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import tornadofx.*
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.util.concurrent.CancellationException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * A copy of [tornadofx.DefaultErrorHandler] that ignores [CancellationException].
 * It exists because CancellationExceptions were being logged to the logger as errors.
 */
class EnhancedDefaultErrorHandler : Thread.UncaughtExceptionHandler {
    private val log = Logger.getLogger("ErrorHandler")

    override fun uncaughtException(t: Thread, error: Throwable) {
        if (error is CancellationException) return

        log.log(Level.SEVERE, "Uncaught error", error)

        if (isCycle(error)) {
            log.log(Level.INFO, "Detected cycle handling error, aborting.", error)
        } else {
            javaFx {
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
                if (error is RestException) {
                    try {

                        title = "HTTP Request Error: $title"
                        form {
                            fieldset(error.message) {
                                val response = error.response
                                if (response != null) {
                                    field("Status") {
                                        label("${response.statusCode} ${response.reason}")
                                    }

                                    val c = response.text()

                                    if (c != null) {
                                        tabpane {
                                            background = Color.TRANSPARENT.asBackground()

                                            tab("Plain text") {
                                                textarea(c)
                                            }
                                            tab("HTML") {
                                                if (response.header("Content-Type")?.contains("html", true) == true)
                                                    select()

                                                webview {
                                                    engine.loadContent(c)
                                                }
                                            }
                                            tab("Stacktrace") {
                                                add(textarea)
                                            }
                                            tabs.withEach { isClosable = false }
                                        }
                                    } else {
                                        add(textarea)
                                    }
                                } else {
                                    add(textarea)
                                }
                            }
                        }

                    } catch (e: Exception) {
                        add(textarea)
                    }
                } else {
                    add(textarea)
                }
            }

            showAndWait()
        }
    }

}

private fun stringFromError(e: Throwable): String {
    val out = ByteArrayOutputStream()
    val writer = PrintWriter(out)
    e.printStackTrace(writer)
    writer.close()
    return out.toString()
}
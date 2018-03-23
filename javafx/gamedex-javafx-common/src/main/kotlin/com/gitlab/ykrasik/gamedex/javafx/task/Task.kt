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

package com.gitlab.ykrasik.gamedex.javafx.task

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.Theme.Images
import com.gitlab.ykrasik.gamedex.javafx.notification.Notifier
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.StringProperty
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.javafx.JavaFx
import org.slf4j.Logger
import tornadofx.*
import java.util.concurrent.CancellationException

/**
 * User: ykrasik
 * Date: 16/03/2017
 * Time: 18:04
 */
// TODO: Add a progress counter after the progressBar.
abstract class Task<out T>(val titleProperty: ThreadAwareStringProperty, private val notifier: Notifier) {
    constructor(title: String, notifier: Notifier) : this(ThreadAwareStringProperty(title), notifier)

    protected val log = logger()

    private lateinit var _result: Deferred<T>
    val result: Deferred<T> get() = _result

    val progress = Progress(log)

    private var loadingGraphic: ImageView by singleAssign()

    private val platformProperty = ThreadAwareObjectProperty<Platform?>()
    var platform by platformProperty

    private val providerLogoProperty = ThreadAwareObjectProperty<Image?>()
    var providerLogo by providerLogoProperty

    val runningProperty = SimpleBooleanProperty(false)

    fun start() {
        _result = async(CommonPool) {
            withContext(JavaFx) {
                runningProperty.set(true)
                notifier.showPersistentNotification(GridPane().apply {
                    paddingAll = 10.0
                    hgap = 10.0
                    vgap = 5.0
                    row {
                        label(titleProperty) {
                            minWidth = 170.0
                        }
                        progressbar(progress.progressProperty) {
                            prefWidth = screenBounds.width
                            gridpaneConstraints { hAlignment = HPos.CENTER }
                        }
                        button(graphic = Images.error.toImageView(height = 30, width = 30)) {
                            setOnAction { _result.cancel() }
                        }
                    }
                    gridpane {
                        gridpaneConstraints { columnRowIndex(0, 1); hAlignment = HPos.LEFT; vAlignment = VPos.CENTER }
                        alignment = Pos.CENTER_LEFT
                        hgap = 5.0
                        fun redraw() {
                            this@gridpane.replaceChildren {
                                row {
                                    platform?.let {
                                        children += it.toLogo(38.0)
                                    }
                                    providerLogo?.let {
                                        children += it.toImageView(height = 40, width = 160)
                                    }
                                }
                            }
                        }

                        platformProperty.perform { redraw() }
                        providerLogoProperty.perform { redraw() }
                    }
                    text(progress.messageProperty) {
                        gridpaneConstraints { columnRowIndex(1, 1); hAlignment = HPos.CENTER }
                    }
                    loadingGraphic = Images.loading.toImageView(height = 40, width = 40).apply {
                        gridpaneConstraints { columnRowIndex(2, 1); }
                    }
                })
            }

            try {
                doRun()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
                }
                throw e
            } finally {
//              FIXME: use  withContext(JavaFx) {
                runLaterIfNecessary {
                    loadingGraphic.isVisible = false
                    runningProperty.set(false)
                    notifier.hidePersistentNotification()
                    notifier.showInfoNotification(doneMessage())
                }
            }
        }
    }

    protected abstract suspend fun CoroutineScope.doRun(): T
    protected abstract fun doneMessage(): String

    class Progress(private val log: Logger?) {
        val messageProperty: StringProperty = ThreadAwareStringProperty()
        val progressProperty: DoubleProperty = ThreadAwareDoubleProperty()

        var message: String
            get() = messageProperty.get()
            set(value) {
                messageProperty.set(value)
                log?.info(value)
            }

        var progress: Double by progressProperty

        fun progress(done: Int, total: Int) {
            progress = done.toDouble() / total.toDouble()
        }
    }
}
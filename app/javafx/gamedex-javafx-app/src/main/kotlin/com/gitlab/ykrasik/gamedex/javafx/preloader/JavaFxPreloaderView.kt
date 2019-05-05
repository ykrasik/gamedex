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

package com.gitlab.ykrasik.gamedex.javafx.preloader

import com.gitlab.ykrasik.gamedex.app.api.preloader.Preloader
import com.gitlab.ykrasik.gamedex.app.api.preloader.PreloaderView
import com.gitlab.ykrasik.gamedex.app.javafx.MainView
import com.gitlab.ykrasik.gamedex.javafx.EnhancedDefaultErrorHandler
import com.gitlab.ykrasik.gamedex.javafx.Main
import com.gitlab.ykrasik.gamedex.javafx.control.asPercent
import com.gitlab.ykrasik.gamedex.javafx.control.clipRectangle
import com.gitlab.ykrasik.gamedex.javafx.control.defaultHbox
import com.gitlab.ykrasik.gamedex.javafx.control.jfxProgressBar
import com.gitlab.ykrasik.gamedex.javafx.module.GuiceDiContainer
import com.gitlab.ykrasik.gamedex.javafx.module.JavaFxModule
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import tornadofx.*
import java.util.*

/**
 * User: ykrasik
 * Date: 01/04/2017
 * Time: 21:53
 */
class JavaFxPreloaderView : View("GameDex"), PreloaderView {
    private var logo = resources.image("gamedex.jpg")

    private val progressProperty = SimpleDoubleProperty(0.0)
    override var progress by progressProperty

    private val messageProperty = SimpleStringProperty()
    override var message by messageProperty

    override val root = stackpane {
        alignment = Pos.CENTER
        group {
            // Groups don't fill their parent's size, which is exactly what we want here.
            vbox(spacing = 5) {
                paddingAll = 5.0
                imageview {
                    image = logo

                    clipRectangle {
                        arcWidth = 14.0
                        arcHeight = 14.0
                        heightProperty().bind(logo.heightProperty())
                        widthProperty().bind(logo.widthProperty())
                    }
                }
                jfxProgressBar(progressProperty) {
                    useMaxWidth = true
                }
                defaultHbox {
                    label(messageProperty) {
                        style {
                            fontSize = 28.px
                        }
                    }
                    spacer()
                    label(progressProperty.asPercent()) {
                        style {
                            fontSize = 28.px
                        }
                    }
                }
            }
        }
    }

    override fun onDock() {
        primaryStage.isMaximized = true

        Thread.setDefaultUncaughtExceptionHandler(EnhancedDefaultErrorHandler())

        GlobalScope.launch(Dispatchers.IO) {
            val preloader = ServiceLoader.load(Preloader::class.java).iterator().next()
            val injector = preloader.load(this@JavaFxPreloaderView, JavaFxModule)
            FX.dicontainer = GuiceDiContainer(injector)
            withContext(Dispatchers.JavaFx) {
                message = "Loading user interface..."
                delay(5)       // Delay to allow the 'done' message to display.
                replaceWith(find(MainView::class, params = mapOf(MainView.StartTimeParam to Main.startTime)), ViewTransition.Fade(1.seconds))
            }
        }
    }
}
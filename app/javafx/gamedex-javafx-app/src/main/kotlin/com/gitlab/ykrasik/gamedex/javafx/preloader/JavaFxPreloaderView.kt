/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.Version
import com.gitlab.ykrasik.gamedex.app.api.preloader.Preloader
import com.gitlab.ykrasik.gamedex.app.api.preloader.PreloaderView
import com.gitlab.ykrasik.gamedex.app.javafx.MainView
import com.gitlab.ykrasik.gamedex.javafx.Main
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.module.GuiceDiContainer
import com.gitlab.ykrasik.gamedex.javafx.module.JavaFxModule
import com.gitlab.ykrasik.gamedex.javafx.mutableStateFlow
import com.gitlab.ykrasik.gamedex.javafx.typesafeStringBinding
import javafx.geometry.Pos
import javafx.scene.text.FontWeight
import kotlinx.coroutines.*
import tornadofx.*
import java.util.*

/**
 * User: ykrasik
 * Date: 01/04/2017
 * Time: 21:53
 */
class JavaFxPreloaderView : View("GameDex"), PreloaderView {
    private var logo = resources.image("gamedex.jpg")

    override val version = mutableStateFlow(Version.Null, debugName = "version")
    override val progress = mutableStateFlow(0.0, debugName = "progress")
    override val message = mutableStateFlow("", debugName = "message")

    override val root = stackpane {
        alignment = Pos.CENTER
        group {
            // Groups don't fill their parent's size, which is exactly what we want here.
            stackpane {
                defaultVbox {
                    paddingAll = 5
                    imageview {
                        image = logo

                        clipRectangle {
                            arcWidth = 14.0
                            arcHeight = 14.0
                            heightProperty().bind(logo.heightProperty())
                            widthProperty().bind(logo.widthProperty())
                        }
                    }
                    jfxProgressBar(progress.property) {
                        useMaxWidth = true
                    }
                    defaultHbox {
                        label(message.property) {
                            style {
                                fontSize = 28.px
                            }
                        }
                        spacer()
                        label(progress.property.asPercent()) {
                            style {
                                fontSize = 28.px
                            }
                        }
                    }
                }

                defaultHbox(alignment = Pos.TOP_RIGHT) {
                    paddingAll = 15
                    label("Version") {
                        style {
                            fontWeight = FontWeight.BOLD
                        }
                    }
                    label(version.property.typesafeStringBinding { it.version })
                }
            }
        }
    }

    override fun onDock() {
        primaryStage.isMaximized = true

        GlobalScope.launch(Dispatchers.IO + CoroutineName("Preloader")) {
            val preloader = ServiceLoader.load(Preloader::class.java).iterator().next()
            val injector = preloader.load(this@JavaFxPreloaderView, JavaFxModule)
            FX.dicontainer = GuiceDiContainer(injector)
            message.value = "Loading user interface..."
            withContext(Dispatchers.Main) {
                delay(5)       // Delay to allow the 'done' message to display.
                replaceWith(find(MainView::class, params = mapOf(MainView.StartTimeParam to Main.timeMark)), ViewTransition.Fade(1.seconds))
            }
        }
    }
}
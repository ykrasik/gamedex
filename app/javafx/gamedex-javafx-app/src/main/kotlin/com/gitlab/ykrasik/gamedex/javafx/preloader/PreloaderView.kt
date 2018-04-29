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

import com.gitlab.ykrasik.gamedex.core.preloader.Preloader
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.module.GuiceDiContainer
import com.gitlab.ykrasik.gamedex.javafx.module.JavaFxModule
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import tornadofx.*

/**
 * User: ykrasik
 * Date: 01/04/2017
 * Time: 21:53
 */
class PreloaderView : View("GameDex") {
    private var logo = resources.image("gamedex.jpg")
    private val progressProperty = SimpleDoubleProperty()
    private val messageProperty = SimpleStringProperty()

    override val root = vbox(spacing = 5) {
        paddingAll = 5.0
        imageview {
            image = logo

            clipRectangle {
                arcWidth = 10.0
                arcHeight = 10.0
                heightProperty().bind(logo.heightProperty())
                widthProperty().bind(logo.widthProperty())
            }
        }
        progressbar(progressProperty) { useMaxWidth = true }
        label(messageProperty)
    }

    override fun onDock() {
        primaryStage.x = screenBounds.minX + screenBounds.width / 2 - logo.width / 2
        primaryStage.y = screenBounds.minY + screenBounds.height / 3 - logo.height / 2

        Thread.setDefaultUncaughtExceptionHandler(EnhancedDefaultErrorHandler())

        javaFx {
            val task = Preloader.load(JavaFxModule)
            progressProperty.bind(task.progressChannel.toObservableValue(0.0))
            messageProperty.bind(task.message1Channel.toObservableValue(""))
            val injector = withContext(CommonPool) { task.run() }
            FX.dicontainer = GuiceDiContainer(injector)
            replaceWith(MainView::class)
        }
    }
}
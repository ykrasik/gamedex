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

package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamdex.core.api.util.uiThreadDispatcher
import com.gitlab.ykrasik.gamdex.core.api.util.uiThreadScheduler
import com.gitlab.ykrasik.gamedex.ui.view.preloader.PreloaderView
import com.gitlab.ykrasik.gamedex.util.UiLogAppender
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import javafx.application.Application
import kotlinx.coroutines.experimental.javafx.JavaFx
import org.slf4j.bridge.SLF4JBridgeHandler
import tornadofx.App

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 21:40
 */
class Main : App(PreloaderView::class) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            uiThreadScheduler = JavaFxScheduler.platform()
            uiThreadDispatcher = JavaFx
            SLF4JBridgeHandler.removeHandlersForRootLogger()
            SLF4JBridgeHandler.install()
            UiLogAppender.init()

            Application.launch(Main::class.java, *args)
        }
    }
}
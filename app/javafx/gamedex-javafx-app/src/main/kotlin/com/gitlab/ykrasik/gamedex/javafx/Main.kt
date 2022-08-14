/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.javafx.preloader.JavaFxPreloaderView
import com.gitlab.ykrasik.gamedex.javafx.theme.GameDexStyle
import tornadofx.App
import tornadofx.launch
import kotlin.time.TimeSource

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 21:40
 */
class Main : App(JavaFxPreloaderView::class, GameDexStyle::class) {
    companion object {
        val timeMark = TimeSource.Monotonic.markNow()

        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("kotlinx.coroutines.debug", "on")

            Thread.currentThread().contextClassLoader = Main::class.java.classLoader
            launch<Main>(args)
        }
    }
}

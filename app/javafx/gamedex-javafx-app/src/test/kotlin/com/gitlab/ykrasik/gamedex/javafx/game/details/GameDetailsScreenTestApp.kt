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

package com.gitlab.ykrasik.gamedex.javafx.game.details

import com.gitlab.ykrasik.gamedex.javafx.BaseTestApp
import com.gitlab.ykrasik.gamedex.test.randomGame

/**
 * User: ykrasik
 * Date: 01/04/2017
 * Time: 14:06
 */
object GameDetailsScreenTestApp : BaseTestApp<JavaFxGameDetailsScreen>(JavaFxGameDetailsScreen::class) {
    override fun init(view: JavaFxGameDetailsScreen) {
        view.game = randomGame()
    }

    @JvmStatic fun main(args: Array<String>) {}
}
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

package com.gitlab.ykrasik.gamedex.javafx.game.edit

import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.javafx.BaseFragmentTestApp
import com.gitlab.ykrasik.gamedex.test.randomGame

/**
 * User: ykrasik
 * Date: 13/05/2017
 * Time: 12:12
 */
object EditGameDataFragmentTestApp : BaseFragmentTestApp() {
    override fun init() {
        println("Result: " + JavaFxEditGameView().show(randomGame(), initialTab = GameDataType.name_))
    }

    @JvmStatic fun main(args: Array<String>) {}
}
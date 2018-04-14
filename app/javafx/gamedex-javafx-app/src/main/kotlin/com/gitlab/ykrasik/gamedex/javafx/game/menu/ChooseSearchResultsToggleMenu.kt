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

package com.gitlab.ykrasik.gamedex.javafx.game.menu

import com.gitlab.ykrasik.gamedex.core.game.GameUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import com.gitlab.ykrasik.gamedex.javafx.jfxToggleNode
import com.gitlab.ykrasik.gamedex.javafx.toPropertyCached
import javafx.scene.layout.VBox
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 21:15
 */
class ChooseSearchResultsToggleMenu : Fragment() {
    private val userConfigRepository: UserConfigRepository by di()
    private val gameUserConfig = userConfigRepository[GameUserConfig::class]

    override val root = vbox(spacing = 5.0) {
        togglegroup {
            GameUserConfig.ChooseResults.values().forEach { chooseResults ->
                jfxToggleNode(text = chooseResults.description, value = chooseResults) {
                    useMaxWidth = true
                }
            }
            bind(gameUserConfig.chooseResultsSubject.toPropertyCached())
        }
    }

    fun install(vbox: VBox) {
        vbox.children += root
    }
}
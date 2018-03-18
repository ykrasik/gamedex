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

package com.gitlab.ykrasik.gamedex.ui.view.game.menu

import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.jfxToggleNode
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import javafx.scene.layout.VBox
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 21:15
 */
class ChooseSearchResultsToggleMenu : Fragment() {
    private val settings: GameSettings by di()

    override val root = vbox(spacing = 5.0) {
        togglegroup {
            GameSettings.ChooseResults.values().forEach { chooseResults ->
                jfxToggleNode(chooseResults.key) {
                    addClass(CommonStyle.fillAvailableWidth)
                    isSelected = settings.chooseResults == chooseResults
                    selectedProperty().onChange {
                        if (it) settings.chooseResults = chooseResults
                    }
                    settings.chooseResultsProperty.onChange {
                        isSelected = chooseResults == it
                    }
                }
            }
        }
    }

    fun install(vbox: VBox) {
        vbox.children += root
    }
}
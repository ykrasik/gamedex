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

package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.core.Filter
import com.gitlab.ykrasik.gamedex.ui.jfxButton
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import tornadofx.*

/**
 * User: ykrasik
 * Date: 25/06/2017
 * Time: 09:48
 */
class DuplicationFragment(
    duplication: Filter.Duplications.GameDuplication,
    gamesTable: TableView<Game>
) : Fragment() {
    override val root = form {
        addClass(CommonStyle.centered)
        fieldset {
            inputGrow = Priority.ALWAYS
            field {
                val game = duplication.duplicatedGame
                jfxButton(game.name) {
                    addClass(CommonStyle.fillAvailableWidth)
                    setOnAction {
                        gamesTable.selectionModel.select(game)
                        gamesTable.scrollTo(game)
                    }
                }
            }
        }
    }
}
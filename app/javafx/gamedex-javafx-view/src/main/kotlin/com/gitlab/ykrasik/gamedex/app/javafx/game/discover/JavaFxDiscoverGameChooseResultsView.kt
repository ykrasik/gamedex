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

package com.gitlab.ykrasik.gamedex.app.javafx.game.discover

import com.gitlab.ykrasik.gamedex.app.api.game.DiscoverGameChooseResults
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanChangeDiscoverGameChooseResults
import com.gitlab.ykrasik.gamedex.javafx.control.disallowDeselection
import com.gitlab.ykrasik.gamedex.javafx.control.jfxToggleNode
import com.gitlab.ykrasik.gamedex.javafx.userMutableState
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import javafx.event.EventTarget
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 21:15
 */
private class JavaFxDiscoverGameChooseResultsView : PresentableView(), ViewCanChangeDiscoverGameChooseResults {
    override var discoverGameChooseResults = userMutableState(DiscoverGameChooseResults.chooseIfNonExact)

    init {
        register()
    }

    override val root = vbox(spacing = 5.0) {
        togglegroup {
            DiscoverGameChooseResults.values().forEach { chooseResults ->
                jfxToggleNode(text = chooseResults.description, value = chooseResults) {
                    useMaxWidth = true
                }
            }
            disallowDeselection()
            bind(discoverGameChooseResults.property)
        }
    }
}

fun EventTarget.discoverGameChooseResultsMenu() = opcr(this, JavaFxDiscoverGameChooseResultsView().root)
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

import com.gitlab.ykrasik.gamedex.app.api.game.discover.DiscoverGameChoiceConfigPresenter
import com.gitlab.ykrasik.gamedex.app.api.game.discover.DiscoverGameChoiceConfigView
import com.gitlab.ykrasik.gamedex.app.api.game.discover.DiscoverGameChooseResults
import com.gitlab.ykrasik.gamedex.javafx.disallowDeselection
import com.gitlab.ykrasik.gamedex.javafx.jfxToggleNode
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableView
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventTarget
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 21:15
 */
private class JavaFxDiscoverGameChoiceConfigView : PresentableView<DiscoverGameChoiceConfigView.Event>(), DiscoverGameChoiceConfigView {
    private val presenter: DiscoverGameChoiceConfigPresenter by di()

    private val discoverGameChooseResultsProperty = SimpleObjectProperty<DiscoverGameChooseResults>(null).eventOnChange(DiscoverGameChoiceConfigView.Event::DiscoverGameChoiceChanged)
    override var discoverGameChooseResults by discoverGameChooseResultsProperty

    init {
        presenter.present(this)
    }

    override val root = vbox(spacing = 5.0) {
        togglegroup {
            DiscoverGameChooseResults.values().forEach { chooseResults ->
                jfxToggleNode(text = chooseResults.description, value = chooseResults) {
                    useMaxWidth = true
                }
            }
            disallowDeselection()
            bind(discoverGameChooseResultsProperty)
        }
    }
}

fun EventTarget.discoverGameChooseResultsMenu() = opcr(this, JavaFxDiscoverGameChoiceConfigView().root)
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

package com.gitlab.ykrasik.gamedex.app.javafx.game.download

import com.gitlab.ykrasik.gamedex.app.api.game.RedownloadGamesView
import com.gitlab.ykrasik.gamedex.app.api.util.IsValid
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxGameFilterView
import com.gitlab.ykrasik.gamedex.javafx.Icons
import com.gitlab.ykrasik.gamedex.javafx.acceptButton
import com.gitlab.ykrasik.gamedex.javafx.and
import com.gitlab.ykrasik.gamedex.javafx.cancelButton
import com.gitlab.ykrasik.gamedex.javafx.control.enableWhen
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import javafx.beans.property.SimpleObjectProperty
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 10:57
 */
class JavaFxRedownloadGamesView : PresentableView("Re-Download Games", Icons.download), RedownloadGamesView {
    private val filterView = JavaFxGameFilterView(onlyShowConditionsForCurrentPlatform = true)

    override var redownloadGamesCondition by filterView.filterProperty
    override val redownloadGamesConditionChanges = filterView.filterChanges

    private val canAcceptProperty = SimpleObjectProperty(IsValid.valid)
    override var canAccept by canAcceptProperty

    override val acceptActions = channel<Unit>()
    override val cancelActions = channel<Unit>()

    init {
        viewRegistry.onCreate(this)
    }

    override val root = borderpane {
        prefWidth = 600.0
        minHeight = 500.0
        top {
            toolbar {
                cancelButton { eventOnAction(cancelActions) }
                spacer()
                acceptButton {
                    enableWhen(canAcceptProperty.and(filterView.isValid))
                    eventOnAction(acceptActions)
                }
            }
        }
        center = filterView.root.apply {
            paddingAll = 10
        }
    }
}
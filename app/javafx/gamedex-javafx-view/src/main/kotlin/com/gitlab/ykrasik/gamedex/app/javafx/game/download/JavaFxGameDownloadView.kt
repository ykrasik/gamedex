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

import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanRedownloadGames
import com.gitlab.ykrasik.gamedex.app.api.game.ViewWithRedownloadGamesCondition
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxGameFilterView
import com.gitlab.ykrasik.gamedex.javafx.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.Icons
import com.gitlab.ykrasik.gamedex.javafx.confirmButton
import com.gitlab.ykrasik.gamedex.javafx.control.buttonWithPopover
import com.gitlab.ykrasik.gamedex.javafx.control.verticalGap
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import org.controlsfx.control.PopOver
import tornadofx.action
import tornadofx.addClass
import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 10:57
 */
class JavaFxGameDownloadView : PresentableView(), ViewWithRedownloadGamesCondition, ViewCanRedownloadGames {
    private val filterView = JavaFxGameFilterView(onlyShowConditionsForCurrentPlatform = true)

    override var redownloadGamesCondition by filterView.filterProperty
    override val redownloadGamesConditionChanges = filterView.filterChanges

    override val redownloadGamesActions = channel<Unit>()

    init {
        viewRegistry.onCreate(this)
    }

    override val root = buttonWithPopover("Re-Download", graphic = Icons.download, arrowLocation = PopOver.ArrowLocation.TOP_RIGHT, closeOnClick = false) { popover ->
        confirmButton("Re-Download Games Matching this Condition", Icons.download) {
            action {
                redownloadGamesActions.event(Unit)
                popover.hide()
            }

        }
        verticalGap()
        add(filterView)
    }.apply {
        addClass(CommonStyle.toolbarButton)
    }
}
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
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxGameFilterView
import com.gitlab.ykrasik.gamedex.javafx.Icons
import com.gitlab.ykrasik.gamedex.javafx.userMutableState
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import tornadofx.borderpane
import tornadofx.paddingAll
import tornadofx.scrollpane

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 10:57
 */
class JavaFxRedownloadGamesView : ConfirmationWindow("Re-Download Games", Icons.download), RedownloadGamesView {
    private val filterView = JavaFxGameFilterView(onlyShowConditionsForCurrentPlatform = true)

    override val redownloadGamesCondition = userMutableState(filterView.filter)
    override val redownloadGamesConditionIsValid = userMutableState(filterView.filterIsValid)

    init {
        register()
    }

    override val root = borderpane {
        top = confirmationToolbar()
        center = scrollpane {
            paddingAll = 10
            add(filterView.root)
            redownloadGamesCondition.onChange { resizeToContent() }
        }
    }
}
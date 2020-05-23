/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.javafx.provider

import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.provider.SyncGamesWithMissingProvidersView
import com.gitlab.ykrasik.gamedex.app.api.util.ValidatedValue
import com.gitlab.ykrasik.gamedex.app.api.util.fromView
import com.gitlab.ykrasik.gamedex.app.api.util.writeFrom
import com.gitlab.ykrasik.gamedex.app.api.util.writeTo
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxFilterView
import com.gitlab.ykrasik.gamedex.javafx.addComponent
import com.gitlab.ykrasik.gamedex.javafx.control.defaultVbox
import com.gitlab.ykrasik.gamedex.javafx.control.jfx2SideToggleButton
import com.gitlab.ykrasik.gamedex.javafx.control.prettyScrollPane
import com.gitlab.ykrasik.gamedex.javafx.screenBounds
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.javafx.viewMutableStateFlow
import com.gitlab.ykrasik.gamedex.util.IsValid
import tornadofx.borderpane
import tornadofx.paddingAll
import tornadofx.tooltip

/**
 * User: ykrasik
 * Date: 31/12/2018
 * Time: 16:25
 */
class JavaFxSyncGamesWithMissingProvidersView : ConfirmationWindow("Sync Games with Missing Providers", Icons.sync), SyncGamesWithMissingProvidersView {
    private val filterView = JavaFxFilterView()

    override val bulkSyncGamesFilter = viewMutableStateFlow(Filter.Null, debugName = "bulkSyncGamesFilter")
        .writeTo(filterView.filter) { it.asFromView() }
        .writeFrom(filterView.filter) { it.asFromView() }

    override val bulkSyncGamesFilterValidatedValue = viewMutableStateFlow(ValidatedValue(Filter.Null, IsValid.valid), debugName = "bulkSyncGamesFilterValidatedValue")
        .writeFrom(filterView.filterValidatedValue) { it.fromView }

    override val syncOnlyMissingProviders = viewMutableStateFlow(true, debugName = "syncOnlyMissingProviders")

    init {
        register()
    }

    override val root = borderpane {
        maxHeight = screenBounds.height * 2 / 3
        top = confirmationToolbar()
        center = defaultVbox(spacing = 20) {
            paddingAll = 20
            jfx2SideToggleButton(
                syncOnlyMissingProviders.property,
                checkedText = "Sync only missing providers",
                uncheckedText = "Sync all providers"
            ) {
                tooltip("If checked, only the missing providers of the game will be synced. Otherwise, all providers in a game that has missing providers will be synced.")
            }
            prettyScrollPane {
                isFitToWidth = true
                addComponent(filterView)
            }
        }
    }
}
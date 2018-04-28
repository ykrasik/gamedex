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

import com.gitlab.ykrasik.gamedex.core.FilterSet
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderRepository
import com.gitlab.ykrasik.gamedex.core.api.util.value_
import com.gitlab.ykrasik.gamedex.core.game.Filter
import com.gitlab.ykrasik.gamedex.core.game.GameUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.game.GameController
import com.gitlab.ykrasik.gamedex.javafx.game.filter.FilterFragment
import org.controlsfx.control.textfield.CustomTextField
import tornadofx.*

/**
 * User: ykrasik
 * Date: 02/06/2017
 * Time: 19:48
 */
class GameFilterMenu : View() {
    private val gameController: GameController by di()
    private val libraryService: LibraryService by di()
    private val providerRepository: GameProviderRepository by di()
    private val userConfigRepository: UserConfigRepository by di()
    private val gameUserConfig = userConfigRepository[GameUserConfig::class]

    private val filterSet = FilterSet.Builder(gameUserConfig, libraryService, gameController, providerRepository)
        .without(Filter.Platform::class, Filter.Duplications::class, Filter.NameDiff::class)
        .build()

    override val root = buttonWithPopover("Filter", Theme.Icon.filter(), closeOnClick = false) {
        form {
            fieldset {
                clearAllButton()
                separator()
                searchText()
                separator()
                filter()
            }
        }
    }.apply {
        shortcut("ctrl+f")
        tooltip("Ctrl+f")
    }

    private fun Fieldset.clearAllButton() = field {
        jfxButton("Clear all", Theme.Icon.clear(22.0)) {
            addClass(CommonStyle.fillAvailableWidth)
            isCancelButton = true
            isFocusTraversable = false
            setOnAction { gameController.clearFilters() }
        }
    }

    // TODO: Move this out of the menu into the toolbar.
    private fun Fieldset.searchText() {
        field("Search") {
            val search = CustomTextField().apply {
                addClass(CommonStyle.fillAvailableWidth)
                promptText = "Search"
                left = Theme.Icon.search(18.0)
                gameController.searchQueryProperty.bindBidirectional(textProperty())
                requestFocus()
            }
            addChildIfPossible(search)
        }.apply {
            label.replaceWith(jfxButton("Search") {
                isFocusTraversable = false
                mouseTransparentWhen { gameController.searchQueryProperty.isEmpty }
                setOnAction { gameController.searchQueryProperty.value = "" }
            })
        }
    }

    private fun Fieldset.filter() = field {
        vbox {
            val filterProperty = gameUserConfig.currentPlatformFilterSubject
            val fragment = FilterFragment(filterProperty, filterSet)
            fragment.newFilterObservable.subscribe {
                filterProperty.value_ = it
            }
            children += fragment.root
        }
    }
}
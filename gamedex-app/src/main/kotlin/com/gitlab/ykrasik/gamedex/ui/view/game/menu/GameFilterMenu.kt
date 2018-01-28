package com.gitlab.ykrasik.gamedex.ui.view.game.menu

import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.core.Filter
import com.gitlab.ykrasik.gamedex.core.FilterSet
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.settings.setFilter
import com.gitlab.ykrasik.gamedex.ui.buttonWithPopover
import com.gitlab.ykrasik.gamedex.ui.jfxButton
import com.gitlab.ykrasik.gamedex.ui.mouseTransparentWhen
import com.gitlab.ykrasik.gamedex.ui.perform
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.view.game.filter.FilterFragment
import org.controlsfx.control.textfield.CustomTextField
import tornadofx.*

/**
 * User: ykrasik
 * Date: 02/06/2017
 * Time: 19:48
 */
class GameFilterMenu : View() {
    private val gameController: GameController by di()
    private val libraryController: LibraryController by di()
    private val providerRepository: GameProviderRepository by di()
    private val settings: GameSettings by di()

    private val filterSet = FilterSet.Builder(settings, libraryController, gameController, providerRepository)
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
            val filterProperty = settings.filterForCurrentPlatformProperty.value.toProperty()
            filterProperty.perform {
                replaceChildren {
                    children += FilterFragment(filterProperty, filterSet).root
                }
            }

            filterProperty.onChange {settings.setFilter(it!!) }
        }
    }
}
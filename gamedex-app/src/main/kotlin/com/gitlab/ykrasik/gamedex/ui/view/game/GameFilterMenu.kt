package com.gitlab.ykrasik.gamedex.ui.view.game

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import org.controlsfx.control.PopOver
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

    override val root = buttonWithPopover("Filter", Theme.Icon.filter(), closeOnClick = false) {
        form {
            fieldset {
                clearAllButton()
                separator()
                searchText()
                separator()
                libraryFilter()
                genreFilter()
                tagFilter()
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
            children += search
        }.apply {
            label.replaceWith(jfxButton("Search") {
                isFocusTraversable = false
                mouseTransparentWhen { gameController.searchQueryProperty.isEmpty }
                setOnAction { gameController.searchQueryProperty.value = "" }
            })
        }
    }

    private fun Fieldset.libraryFilter() {
        val filteredLibraries = gameController.filteredLibrariesProperty.map { libraryIds ->
            libraryIds!!.map { id ->
                libraryController.realLibraries.find { it.id == id }!!
            }
        }
        filteredLibraries.onChange { gameController.filteredLibrariesProperty.value = it!!.map { it.id } }

        val shouldShow = libraryController.platformLibraries.mapProperty { it.size > 1 }

        field("Libraries") {
            popoverToggleMenu(
                possibleItems = libraryController.platformLibraries,
                selectedItems = filteredLibraries,
                arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                styleClasses = listOf(CommonStyle.fillAvailableWidth),
                itemStyleClasses = listOf(Style.filterItem),
                text = Library::name
            )
        }.apply {
            showWhen { shouldShow }
            // TODO: Display a 'clear' graphic on mouse over
            label.replaceWith(jfxButton("Libraries") {
                isFocusTraversable = false
                mouseTransparentWhen { gameController.filteredLibrariesProperty.booleanBinding { it!!.isEmpty() } }
                setOnAction { gameController.filterLibraries(emptyList()) }
            })
        }

        separator { showWhen { shouldShow } }
    }

    private fun Fieldset.genreFilter() {
        val platformGenres = gameController.platformGames.flatMapping { it.genres }.distincting().sortedFiltered()
        platformGenres.sortedItems.setComparator { o1, o2 -> o1.compareTo(o2) }

        val shouldShow = platformGenres.mapProperty { it.size > 1 }

        field("Genres") {
            popoverToggleMenu(
                possibleItems = platformGenres,
                selectedItems = gameController.filteredGenresProperty,
                arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                styleClasses = listOf(CommonStyle.fillAvailableWidth),
                itemStyleClasses = listOf(Style.filterItem),
                text = { it }
            )
        }.apply {
            showWhen { shouldShow }
            label.replaceWith(jfxButton("Genres") {
                isFocusTraversable = false
                mouseTransparentWhen { gameController.filteredGenresProperty.booleanBinding { it!!.isEmpty() } }
                setOnAction { gameController.filterGenres(emptyList()) }
            })
        }

        separator { showWhen { shouldShow } }
    }

    private fun Fieldset.tagFilter() {
        val platformTags = gameController.platformGames.flatMapping { it.tags }.distincting().sortedFiltered()
        platformTags.sortedItems.setComparator { o1, o2 -> o1.compareTo(o2) }

        val shouldShow = platformTags.mapProperty { it.isNotEmpty() }

        field("Tags") {
            popoverToggleMenu(
                possibleItems = platformTags,
                selectedItems = gameController.filteredTagsProperty,
                arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                styleClasses = listOf(CommonStyle.fillAvailableWidth),
                itemStyleClasses = listOf(Style.filterItem),
                text = { it }
            )
        }.apply {
            showWhen { shouldShow }
            label.replaceWith(jfxButton("Tags") {
                isFocusTraversable = false
                mouseTransparentWhen { gameController.filteredTagsProperty.booleanBinding { it!!.isEmpty() } }
                setOnAction { gameController.filterTags(emptyList()) }
            })
        }

        separator { showWhen { shouldShow } }
    }

    class Style : Stylesheet() {
        companion object {
            val filterItem by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            filterItem {
                minWidth = 160.px
            }
        }
    }
}
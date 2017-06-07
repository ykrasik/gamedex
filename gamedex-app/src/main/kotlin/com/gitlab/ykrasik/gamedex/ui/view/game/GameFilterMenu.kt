package com.gitlab.ykrasik.gamedex.ui.view.game

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import javafx.event.EventTarget
import javafx.scene.layout.Pane
import org.controlsfx.control.PopOver
import org.controlsfx.control.textfield.CustomTextField
import org.controlsfx.control.textfield.TextFields
import tornadofx.*

/**
 * User: ykrasik
 * Date: 02/06/2017
 * Time: 19:48
 */
class GameFilterMenu : View() {
    private val gameController: GameController by di()
    private val libraryController: LibraryController by di()

    // TODO: Add individual clear buttons (genres, tags).

    override val root = buttonWithPopover("Filter", Theme.Icon.filter(), closeOnClick = false) {
        form {
            fieldset {
                field { clearAllButton() }
                separator()
                field("Search") { searchText() }
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

    private fun EventTarget.clearAllButton() {
        jfxButton("Clear all", Theme.Icon.clear(22.0)) {
            addClass(CommonStyle.fillAvailableWidth)
            isCancelButton = true
            isFocusTraversable = false
            setOnAction { gameController.clearFilters() }
        }
    }

    private fun Pane.searchText() {
        val search = (TextFields.createClearableTextField() as CustomTextField).apply {
            addClass(CommonStyle.fillAvailableWidth)
            promptText = "Search"
            left = Theme.Icon.search(18.0)
            gameController.searchQueryProperty.bindBidirectional(textProperty())
            requestFocus()
        }
        children += search
    }

    private fun Fieldset.libraryFilter() {
        val selectedLibraries = gameController.filteredLibrariesProperty.map { libraryIds ->
            libraryIds!!.map { filteredLibraryId ->
                libraryController.realLibraries.find { it.id == filteredLibraryId }!!
            }
        }
        selectedLibraries.onChange { gameController.filteredLibrariesProperty.value = it!!.map { it.id } }

        val shouldShow = libraryController.platformLibraries.mapProperty { it.size > 1 }

        field("Library") {
            popoverToggleMenu(
                possibleItems = libraryController.platformLibraries,
                selectedItems = selectedLibraries,
                arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                styleClasses = listOf(CommonStyle.fillAvailableWidth),
                itemStyleClasses = listOf(Style.filterItem),
                text = Library::name
            )
        }.apply {
            showWhen { shouldShow }
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
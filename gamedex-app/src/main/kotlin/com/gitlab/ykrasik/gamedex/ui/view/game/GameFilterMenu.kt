package com.gitlab.ykrasik.gamedex.ui.view.game

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import javafx.event.EventTarget
import javafx.geometry.Pos
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
    private val gameRepository: GameRepository by di()

    private val libraries = libraryController.libraries.filtered { it.platform != Platform.excluded }
    private val platformFilterProperty = gameController.sortedFilteredGames.platformProperty

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
        // FIXME: Doesn't deselect items
        jfxButton("Clear all", Theme.Icon.clear(22.0)) {
            addClass(Style.filterButton)
            isCancelButton = true
            isFocusTraversable = false
            setOnAction { gameController.sortedFilteredGames.clearFilters() }
        }
    }

    private fun Pane.searchText() {
        val search = (TextFields.createClearableTextField() as CustomTextField).apply {
            addClass(Style.filterButton)
            promptText = "Search"
            left = Theme.Icon.search(18.0)
            gameController.sortedFilteredGames.searchQueryProperty.bindBidirectional(textProperty())
            requestFocus()
        }
        children += search
    }

    private fun Fieldset.libraryFilter() {
        val platformLibraries = libraries.filtering(platformFilterProperty.toPredicateF { platform, library: Library ->
            library.platform == platform
        })
        val selectedLibraries = gameController.sortedFilteredGames.filtersForPlatformProperty.map { filters ->
            filters!!.libraries.map { filteredLibraryId ->
                libraries.find { it.id == filteredLibraryId }!!
            }
        }
        selectedLibraries.onChange { gameController.sortedFilteredGames.filterLibraries(it!!) }

        val shouldShow = platformLibraries.mapProperty { it.size > 1 }

        field("Library") {
            popoverToggleMenu(
                possibleItems = platformLibraries,
                selectedItems = selectedLibraries,
                arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                styleClasses = listOf(Style.filterButton),
                itemStyleClasses = listOf(Style.filterItem),
                text = Library::name
            )
        }.apply {
            showWhen { shouldShow }
        }

        separator { showWhen { shouldShow } }
    }

    private fun Fieldset.genreFilter() {
        val platformGenres = gameController.genres.filtering(platformFilterProperty.toPredicateF { platform, genre: String ->
            gameRepository.games.any { it.platform == platform && it.genres.contains(genre) }
        }).sortedFiltered()
        platformGenres.sortedItems.setComparator { o1, o2 -> o1.compareTo(o2) }

        val selectedGenres = gameController.sortedFilteredGames.filtersForPlatformProperty.map { filters ->
            filters!!.genres
        }
        selectedGenres.onChange { gameController.sortedFilteredGames.filterGenres(it!!) }

        val shouldShow = platformGenres.mapProperty { it.size > 1 }

        field("Genres") {
            popoverToggleMenu(
                possibleItems = platformGenres,
                selectedItems = selectedGenres,
                arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                styleClasses = listOf(Style.filterButton),
                itemStyleClasses = listOf(Style.filterItem),
                text = { it }
            )
        }.apply {
            showWhen { shouldShow }
        }

        separator {
            showWhen { shouldShow }
        }
    }

    private fun Fieldset.tagFilter() {
        val platformTags = gameController.tags.filtering(platformFilterProperty.toPredicateF { platform, tag: String ->
            gameRepository.games.any { it.platform == platform && it.tags.contains(tag) }
        }).sortedFiltered()
        platformTags.sortedItems.setComparator { o1, o2 -> o1.compareTo(o2) }

        val selectedTags = gameController.sortedFilteredGames.filtersForPlatformProperty.map { filters ->
            filters!!.tags
        }

        selectedTags.onChange { gameController.sortedFilteredGames.filterTags(it!!) }

        val shouldShow = platformTags.mapProperty { it.isNotEmpty() }

        field("Tags") {
            popoverToggleMenu(
                possibleItems = platformTags,
                selectedItems = selectedTags,
                arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                styleClasses = listOf(Style.filterButton),
                itemStyleClasses = listOf(Style.filterItem),
                text = { it }
            )
        }.apply {
            showWhen { shouldShow }
        }

        separator {
            showWhen { shouldShow }
        }
    }

    class Style : Stylesheet() {
        companion object {
            val filterButton by cssclass()
            val filterItem by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            filterButton {
                maxWidth = Double.MAX_VALUE.px
                alignment = Pos.CENTER_LEFT
            }

            filterItem {
                minWidth = 160.px
            }
        }
    }
}
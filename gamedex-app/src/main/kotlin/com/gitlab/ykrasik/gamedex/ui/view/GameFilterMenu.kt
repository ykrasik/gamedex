package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.core.SortedFilteredGames
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.theme.toLogo
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

    private val libraries = libraryController.libraries.filtered { it.platform != Platform.excluded }
    private val platformFilterProperty = gameController.sortedFilteredGames.platformFilterProperty

    override val root = buttonWithPopover("Filter", Theme.Icon.filter(), closeOnClick = false) {
        form {
            fieldset {
                field { clearAllButton() }
                separator()
                field("Search") { searchText() }
                separator()
                field("Platform") { platformFilter() }
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
        jfxButton("Clear all", Theme.Icon.clear()) {
            addClass(Style.filterItem)
            isCancelButton = true
            isFocusTraversable = false
            setOnAction { gameController.sortedFilteredGames.clearFilters() }
        }
    }

    private fun Pane.searchText() {
        val search = (TextFields.createClearableTextField() as CustomTextField).apply {
            addClass(Style.filterItem)
            promptText = "Search"
            left = Theme.Icon.search(18.0)
            gameController.sortedFilteredGames.searchQueryProperty.bindBidirectional(textProperty())
            requestFocus()
        }
        children += search
    }

    private fun EventTarget.platformFilter() {
        // SortedFilteredList because regular sortedList doesn't fire changeEvents, for some reason.
        val platformsWithLibraries = libraries.mapping { it.platform }.distincting().sortedFiltered()
        platformsWithLibraries.sortedItems.setComparator { o1, o2 -> o1.key.compareTo(o2.key) }

        popoverComboMenu(
            possibleItems = platformsWithLibraries,
            selectedItemProperty = platformFilterProperty,
            arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
            styleClass = Style.filterItem,
            itemStyleClass = Style.filterItem,
            text = Platform::key,
            graphic = { it.toLogo() }
        )
    }

    private fun Fieldset.libraryFilter() {
        val librariesWithPlatform = libraries.filtering(platformFilterProperty.toPredicateF { platform, library: Library ->
            library.platform == platform
        })

        val shouldShow = librariesWithPlatform.mapProperty { it.size > 1 }

        field("Library") {
            librariesWithPlatform.performing { librariesWithPlatform ->
                replaceChildren {
                    if (!shouldShow.value) return@replaceChildren

                    val selectedLibraries = gameController.sortedFilteredGames.sourceIdsFilterProperty.mapping { sourceId ->
                        libraries.find { it.id == sourceId }!!
                    }
                    popoverToggleMenu(
                        possibleItems = librariesWithPlatform,
                        selectedItems = selectedLibraries,
                        arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                        styleClass = Style.filterItem,
                        itemStyleClass = Style.filterItem,
                        text = Library::name
                    )
                    selectedLibraries.onChange {
                        gameController.sortedFilteredGames.sourceIdsPerPlatformFilter +=
                            gameController.sortedFilteredGames.platformFilter to it!!.map { it.id }
                    }
                }
            }
        }.apply {
            showWhen { shouldShow }
        }

        separator {
            showWhen { shouldShow }
        }
    }

    // TODO: Genres per platform
    private fun Fieldset.genreFilter() {
        // SortedFilteredList because regular sortedList doesn't fire changeEvents, for some reason.
        val genres = gameController.genres.sortedFiltered()
        genres.sortedItems.setComparator { o1, o2 -> o1.compareTo(o2) }

        val shouldShow = genres.mapProperty { it.isNotEmpty() }

        field("Genres") {
            popoverComboMenu(
                possibleItems = listOf(SortedFilteredGames.allGenres).observable().adding(genres),
                selectedItemProperty = gameController.sortedFilteredGames.genreFilterProperty,
                arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                styleClass = Style.filterItem,
                itemStyleClass = Style.filterItem,
                text = { it },
                menuOp = {
                    if (it == SortedFilteredGames.allGenres) {
                        separator()
                    }
                }
            )
        }.apply {
            showWhen { shouldShow }
        }

        separator {
            showWhen { shouldShow }
        }
    }

    // TODO: tags per platform
    private fun Fieldset.tagFilter() {
        // SortedFilteredList because regular sortedList doesn't fire changeEvents, for some reason.
        val tags = gameController.tags.sortedFiltered()
        tags.sortedItems.setComparator { o1, o2 -> o1.compareTo(o2) }

        val shouldShow = tags.mapProperty { it.isNotEmpty() }

        field("Tag") {
            popoverComboMenu(
                possibleItems = listOf(SortedFilteredGames.allTags).observable().adding(tags),
                selectedItemProperty = gameController.sortedFilteredGames.tagFilterProperty,
                arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
                styleClass = Style.filterItem,
                itemStyleClass = Style.filterItem,
                text = { it },
                menuOp = {
                    if (it == SortedFilteredGames.allTags) {
                        separator()
                    }
                }
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
            val filterItem by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            filterItem {
                maxWidth = Double.MAX_VALUE.px
                alignment = Pos.CENTER_LEFT
            }
        }
    }
}
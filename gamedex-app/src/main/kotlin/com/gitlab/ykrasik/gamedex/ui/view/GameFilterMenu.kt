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
import javafx.scene.layout.Priority
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
                inputGrow = Priority.ALWAYS
                field { clearAllButton() }
                separator()
                field("Search") { searchText() }
                separator()
                field("Platform") { platformFilter() }
                separator()
                libraryFilter()
                field("Genre") { genreFilter() }
                separator()
                field("Tag") { tagFilter() }
                separator()
            }
        }
    }.apply {
        shortcut("ctrl+f")
        tooltip("Ctrl+f")
    }

    private fun EventTarget.clearAllButton() {
        jfxButton("Clear all", Theme.Icon.clear()) {
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

    private fun EventTarget.platformFilter() {
        // SortedFilteredList because regular sortedList doesn't fire changeEvents, for some reason.
        val platformsWithLibraries = libraries.mapping { it.platform }.distincting().sortedFiltered()
        platformsWithLibraries.sortedItems.setComparator { o1, o2 -> o1.key.compareTo(o2.key) }

        popoverComboMenu(
            possibleItems = platformsWithLibraries,
            selectedItemProperty = platformFilterProperty,
            arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
            styleClass = Style.filterButton,
            itemStyleClass = Style.platformItem,
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
                        styleClass = Style.filterButton,
                        itemStyleClass = Style.libraryItem,
                        text = Library::name
                    )
                    selectedLibraries.onChange {
                        gameController.sortedFilteredGames.sourceIdsPerPlatformFilter +=
                            gameController.sortedFilteredGames.platformFilter to it!!.map { it.id }
                    }
                }
            }
        }.apply {
            managedProperty().bind(shouldShow)
            visibleWhen { shouldShow }
        }
        separator {
            managedProperty().bind(shouldShow)
            visibleWhen { shouldShow }
        }
    }

    private fun EventTarget.genreFilter() {
        // SortedFilteredList because regular sortedList doesn't fire changeEvents, for some reason.
        val genres = gameController.genres.sortedFiltered()
        genres.sortedItems.setComparator { o1, o2 -> o1.compareTo(o2) }

        popoverComboMenu(
            possibleItems = listOf(SortedFilteredGames.allGenres).observable().adding(genres),
            selectedItemProperty = gameController.sortedFilteredGames.genreFilterProperty,
            arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
            styleClass = Style.filterButton,
            itemStyleClass = Style.genreItem,
            text = { it },
            menuOp = {
                if (it == SortedFilteredGames.allGenres) {
                    separator()
                }
            }
        )
    }

    // TODO: Hide when no tags.
    private fun EventTarget.tagFilter() {
        // SortedFilteredList because regular sortedList doesn't fire changeEvents, for some reason.
        val tags = gameController.tags.sortedFiltered()
        tags.sortedItems.setComparator { o1, o2 -> o1.compareTo(o2) }

        popoverComboMenu(
            possibleItems = listOf(SortedFilteredGames.allTags).observable().adding(tags),
            selectedItemProperty = gameController.sortedFilteredGames.tagFilterProperty,
            arrowLocation = PopOver.ArrowLocation.LEFT_TOP,
            styleClass = Style.filterButton,
            itemStyleClass = Style.tagItem,
            text = { it },
            menuOp = {
                if (it == SortedFilteredGames.allTags) {
                    separator()
                }
            }
        )
    }

    class Style : Stylesheet() {
        companion object {
            val filterButton by cssclass()
            val platformItem by cssclass()
            val libraryItem by cssclass()
            val genreItem by cssclass()
            val tagItem by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            filterButton {
                maxWidth = Double.MAX_VALUE.px
                alignment = Pos.CENTER_LEFT
            }

            platformItem {
                prefWidth = 100.px
                alignment = Pos.CENTER_LEFT
            }

            libraryItem {
                prefWidth = 160.px
                alignment = Pos.CENTER_LEFT
            }

            genreItem {
                prefWidth = 160.px
                alignment = Pos.CENTER_LEFT
            }

            tagItem {
                prefWidth = 160.px
                alignment = Pos.CENTER_LEFT
            }
        }
    }
}
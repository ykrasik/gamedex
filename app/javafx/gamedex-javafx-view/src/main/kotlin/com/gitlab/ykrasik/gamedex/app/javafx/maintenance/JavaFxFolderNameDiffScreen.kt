/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.javafx.maintenance

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.RenameMoveGameParams
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanRenameMoveGame
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanShowGameDetails
import com.gitlab.ykrasik.gamedex.app.api.game.ViewGameParams
import com.gitlab.ykrasik.gamedex.app.api.maintenance.FolderNameDiff
import com.gitlab.ykrasik.gamedex.app.api.maintenance.FolderNameDiffFilterMode
import com.gitlab.ykrasik.gamedex.app.api.maintenance.FolderNameDiffView
import com.gitlab.ykrasik.gamedex.app.api.maintenance.FolderNameDiffs
import com.gitlab.ykrasik.gamedex.app.api.util.broadcastFlow
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.app.javafx.game.GameContextMenu
import com.gitlab.ykrasik.gamedex.app.javafx.game.GameDetailsSummaryBuilder
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.backButton
import com.gitlab.ykrasik.gamedex.javafx.theme.header
import com.gitlab.ykrasik.gamedex.javafx.theme.size
import com.gitlab.ykrasik.gamedex.javafx.view.InstallableContextMenu
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import difflib.Chunk
import difflib.Delta
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*
import java.util.function.Predicate

/**
 * User: ykrasik
 * Date: 27/04/2019
 * Time: 13:46
 */
class JavaFxFolderNameDiffScreen : PresentableScreen("Folder Name Diffs", Icons.diff),
    FolderNameDiffView,
    ViewCanShowGameDetails,
    ViewCanRenameMoveGame {

    private val gameContextMenu = GameContextMenu()
    private val commonOps: JavaFxCommonOps by di()

    override val diffs = mutableStateFlow(emptyList<FolderNameDiffs>(), debugName = "diffs")
    private val diffsSortedFiltered = diffs.list.sortedFiltered(Comparator.comparing(FolderNameDiffs::name))

//    override val excludeGameActions = broadcastFlow<Game>()

    override val viewGameDetailsActions = broadcastFlow<ViewGameParams>()

    override val searchText = viewMutableStateFlow("", debugName = "searchText")
    override val matchingGame = mutableStateFlow<Game?>(null, debugName = "matchingGame")

    override val filterMode = viewMutableStateFlow(FolderNameDiffFilterMode.None, debugName = "filterMode")
    override val predicate = mutableStateFlow(Predicate<FolderNameDiffs> { true }, debugName = "predicate")

    override val renameMoveGameActions = broadcastFlow<RenameMoveGameParams>()

    override val hideViewActions = broadcastFlow<Unit>()
    override val customNavigationButton = backButton { action(hideViewActions) }

    private val diffContextMenu = object : InstallableContextMenu<Pair<Game, FolderNameDiff>>(Game.Null to FolderNameDiff("", "", "", null)) {
        override val root = vbox {
            jfxButton("Rename to Expected", Icons.folderEdit) {
                action(renameMoveGameActions) { RenameMoveGameParams(data.value.first, initialSuggestion = data.value.second.expectedFolderName) }
            }
            // TODO: Add a 'search only this provider' option
        }
    }

    private val gamesView = prettyListView(diffsSortedFiltered) {
        prettyListCell { diff ->
            text = null
            graphic = GameDetailsSummaryBuilder(diff.game).build()
        }

        gameContextMenu.install(this) { ViewGameParams(selectionModel.selectedItem.game, diffsSortedFiltered.map { it.game }) }
        onUserSelect { viewGameDetailsActions.event(ViewGameParams(selectionModel.selectedItem.game, diffsSortedFiltered.map { it.game })) }
    }

    private val selectedDiff = gamesView.selectionModel.selectedItemProperty()
    private val diffsOfSelectedGame = selectedDiff.mapToList { it?.diffs ?: emptyList() }

    private val diffsView = prettyListView(diffsOfSelectedGame) {
        prettyListCell { diff ->
            text = null
            graphic = HBox().apply {
                spacing = 20.0
                addClass(Style.diffItem)
                gridpane {
                    hgap = 10.0
                    vgap = 3.0

                    stackpane {
                        minWidth = 160.0
                        minHeight = 80.0
                        gridpaneConstraints { rowSpan = 2 }
                        children += commonOps.providerLogo(diff.providerId).toImageView(height = 80.0, width = 160.0)
                    }

                    header("Expected") { gridpaneConstraints { columnRowIndex(1, 0) } }
                    render(diff.expectedFolderName, diff.patch?.deltas ?: emptyList()) { it.revised }.apply {
                        gridpaneConstraints { columnRowIndex(2, 0) }
                    }

                    header("Actual") { gridpaneConstraints { columnRowIndex(1, 1) } }
                    render(diff.folderName, diff.patch?.deltas ?: emptyList()) { it.original }.apply {
                        gridpaneConstraints { columnRowIndex(2, 1) }
                    }

                    if (diff.patch == null) {
                        stackpane {
                            children += Icons.accept.size(50)
                            gridpaneConstraints { columnRowIndex(3, 0); rowSpan = 2 }
                        }
                    }
                }
                diffContextMenu.install(this) { selectedDiff.value.game to diff }
            }
        }
        onUserSelect {
            renameMoveGameActions.event(RenameMoveGameParams(selectedDiff.value.game, initialSuggestion = selectionModel.selectedItem.expectedFolderName))
        }
    }

    override val root = hbox {
        // Left
        vbox {
            hgrow = Priority.ALWAYS
            maxWidth = screenBounds.width / 2
            add(gamesView.apply { vgrow = Priority.ALWAYS })
        }

        gap(size = 10)

        // Right
        vbox {
            hgrow = Priority.ALWAYS
            add(diffsView.apply { vgrow = Priority.ALWAYS })
        }
    }

    init {
        register()

        matchingGame.property.typeSafeOnChange { match ->
            if (match != null) {
                selectGame(match)
            }
        }

        diffsSortedFiltered.filteredItems.predicateProperty().bind(predicate.property)
    }

    private fun selectGame(game: Game) {
        gamesView.selectionModel.select(diffsSortedFiltered.indexOfFirst { it.game.id == game.id })
    }

    private fun EventTarget.render(source: String, deltas: List<Delta<Char>>, chunkExtractor: (Delta<Char>) -> Chunk<Char>) = hbox {
        addClass(Style.diffText)
        var currentIndex = 0
        deltas.sortedBy { chunkExtractor(it).position }.forEach { delta ->
            val chunk = chunkExtractor(delta)

            match(source.substring(currentIndex, chunk.position))
            currentIndex = chunk.position

            diff(source.substring(currentIndex, currentIndex + chunk.size()), delta.type)
            currentIndex += chunk.size()
        }
        match(source.substring(currentIndex, source.length))
    }

    private fun EventTarget.match(text: String) {
        if (text.isEmpty()) return
        label(text) { addClass(Style.match) }
    }

    private fun EventTarget.diff(text: String, type: Delta.TYPE) {
        if (text.isEmpty()) return
        label(text) {
            addClass(
                Style.diff, when (type) {
                    Delta.TYPE.CHANGE -> Style.change
                    Delta.TYPE.DELETE -> Style.delete
                    Delta.TYPE.INSERT -> Style.insert
                }
            )
        }
    }


    override fun HBox.buildToolbar() {
        searchTextField(this@JavaFxFolderNameDiffScreen, searchText.property) { isFocusTraversable = false }

        gap()

        buttonWithPopover(graphic = Icons.filter, closeOnAction = false) {
            enumComboMenu(filterMode.property, graphic = { it.icon })
        }
//        gap()
//        excludeButton {
//            action(excludeGameActions) { selectedDiff.value!!.game }
//            tooltip(selectedDiff.stringBinding { diff ->
//                "Exclude game '${diff?.game?.name}' from duplicates report."
//            })
//        }
        spacer()
        header(diffsSortedFiltered.sizeProperty.typesafeStringBinding { "Diffs: $it" })
        gap()
    }

    private val FolderNameDiffFilterMode.icon: Node
        get() = when (this) {
            FolderNameDiffFilterMode.None -> Icons.accept
            FolderNameDiffFilterMode.IgnoreIfSingleMatch -> Icons.account
            FolderNameDiffFilterMode.IgnoreIfMajorityMatch -> Icons.accounts
        }

    class Style : Stylesheet() {
        companion object {
            val diffItem by cssclass()
            val diffText by cssclass()
            val match by cssclass()
            val diff by cssclass()
            val change by cssclass()
            val insert by cssclass()
            val delete by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            diffItem {
                fontFamily = "Monospaced"
                fontSize = 18.px
            }
            diffText {
                alignment = Pos.CENTER_LEFT
            }
            diff {
                fontWeight = FontWeight.BOLD
            }
            change {
                backgroundColor = multi(Color.ORANGE)
            }
            insert {
                backgroundColor = multi(Color.GREENYELLOW)
            }
            delete {
                backgroundColor = multi(Color.INDIANRED)
            }
        }
    }
}
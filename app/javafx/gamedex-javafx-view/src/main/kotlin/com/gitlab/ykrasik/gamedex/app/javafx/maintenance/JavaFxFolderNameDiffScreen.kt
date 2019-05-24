/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.app.api.maintenance.FolderNameDiffView
import com.gitlab.ykrasik.gamedex.app.api.maintenance.FolderNameDiffs
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.app.javafx.game.GameContextMenu
import com.gitlab.ykrasik.gamedex.app.javafx.game.details.GameDetailsSummaryBuilder
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.backButton
import com.gitlab.ykrasik.gamedex.javafx.theme.header
import com.gitlab.ykrasik.gamedex.javafx.view.InstallableContextMenu
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import difflib.Chunk
import difflib.Delta
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import javafx.scene.text.TextFlow
import tornadofx.*

/**
 * User: ykrasik
 * Date: 27/04/2019
 * Time: 13:46
 */
class JavaFxFolderNameDiffScreen : PresentableScreen("Folder Name Diffs", Icons.diff),
    FolderNameDiffView,
    ViewCanShowGameDetails,
    ViewCanRenameMoveGame {

    private val gameContextMenu: GameContextMenu by inject()
    private val commonOps: JavaFxCommonOps by di()

    override val diffs = mutableListOf<FolderNameDiffs>().asObservable()
//    override val excludeGameActions = channel<Game>()

    override val viewGameDetailsActions = channel<ViewGameParams>()

    override val searchText = userMutableState("")
    override val matchingGame = state<Game?>(null)

    override val renameMoveGameActions = channel<RenameMoveGameParams>()

    override val hideViewActions = channel<Unit>()
    override val customNavigationButton = backButton { action(hideViewActions) }

    private val diffContextMenu = object : InstallableContextMenu<Pair<Game, FolderNameDiff>>() {
        override val root = vbox {
            jfxButton("Rename to Expected", Icons.folderEdit) {
                action(renameMoveGameActions) { RenameMoveGameParams(data.first, initialSuggestion = data.second.expectedFolderName) }
            }
            // TODO: Add a 'search only this provider' option
        }
    }

    private val gamesView = prettyListView(diffs) {
        prettyListCell { diff ->
            text = null
            graphic = GameDetailsSummaryBuilder(diff.game).build()
        }

        gameContextMenu.install(this) { ViewGameParams(selectionModel.selectedItem.game, diffs.map { it.game }) }
        onUserSelect { viewGameDetailsActions.event(ViewGameParams(selectionModel.selectedItem.game, diffs.map { it.game })) }
    }

    private val selectedDiff = gamesView.selectionModel.selectedItemProperty()
    private val diffsOfSelectedGame = selectedDiff.mapToList { it?.diffs ?: emptyList() }

    private val diffsView = prettyListView(diffsOfSelectedGame) {
        prettyListCell { diff ->
            text = null
            graphic = HBox().apply {
                spacing = 20.0
                addClass(Style.diffItem)
                stackpane {
                    minWidth = 160.0
                    alignment = Pos.CENTER
                    children += commonOps.providerLogo(diff.providerId).toImageView(height = 80.0, width = 160.0)
                }
                form {
                    alignment = Pos.CENTER
                    fieldset {
                        horizontalField("Expected") { render(diff.expectedFolderName, diff.patch.deltas) { it.revised } }
                        horizontalField("Actual") { render(diff.folderName, diff.patch.deltas) { it.original } }
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
    }

    private fun selectGame(game: Game) = runLater {
        gamesView.selectionModel.select(diffs.indexOfFirst { it.game.id == game.id })
    }

    private fun EventTarget.render(source: String, deltas: List<Delta<Char>>, chunkExtractor: (Delta<Char>) -> Chunk<Char>) = textflow {
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

    private fun TextFlow.match(text: String) = label(text) { addClass(Style.match) }

    private fun TextFlow.diff(text: String, type: Delta.TYPE) = label(text) {
        addClass(
            Style.match, Style.diff, when (type) {
                Delta.TYPE.CHANGE -> Style.change
                Delta.TYPE.DELETE -> Style.delete
                Delta.TYPE.INSERT -> Style.insert
            }
        )
    }


    override fun HBox.buildToolbar() {
        searchTextField(this@JavaFxFolderNameDiffScreen, searchText.property) { isFocusTraversable = false }
//        gap()
//        excludeButton {
//            action(excludeGameActions) { selectedDiff.value!!.game }
//            tooltip(selectedDiff.stringBinding { diff ->
//                "Exclude game '${diff?.game?.name}' from duplicates report."
//            })
//        }
        spacer()
        header(diffs.sizeProperty.stringBinding { "Diffs: $it" })
        gap()
    }

    class Style : Stylesheet() {
        companion object {
            val diffItem by cssclass()
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
                fontSize = 18.px
            }
            match {
                fontSize = 18.px
                fontFamily = "Monospaced"
            }
            diff {
                fontWeight = FontWeight.BOLD
            }
            change {
                backgroundColor = multi(Color.ORANGE)
            }
            insert {
                backgroundColor = multi(Color.GREEN)
            }
            delete {
                backgroundColor = multi(Color.INDIANRED)
            }
        }
    }
}
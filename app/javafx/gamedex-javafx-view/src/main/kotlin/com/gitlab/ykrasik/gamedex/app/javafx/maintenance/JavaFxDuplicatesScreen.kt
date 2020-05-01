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

package com.gitlab.ykrasik.gamedex.app.javafx.maintenance

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanShowGameDetails
import com.gitlab.ykrasik.gamedex.app.api.game.ViewGameParams
import com.gitlab.ykrasik.gamedex.app.api.maintenance.DuplicatesView
import com.gitlab.ykrasik.gamedex.app.api.maintenance.GameDuplicates
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.app.javafx.game.GameContextMenu
import com.gitlab.ykrasik.gamedex.app.javafx.game.GameDetailsSummaryBuilder
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.backButton
import com.gitlab.ykrasik.gamedex.javafx.theme.header
import com.gitlab.ykrasik.gamedex.javafx.theme.size
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import javafx.geometry.Pos
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import tornadofx.*

/**
 * User: ykrasik
 * Date: 26/04/2019
 * Time: 08:07
 */
class JavaFxDuplicatesScreen : PresentableScreen("Duplicates", Icons.copy),
    DuplicatesView,
    ViewCanShowGameDetails {

    private val gameContextMenu = GameContextMenu()
    private val commonOps: JavaFxCommonOps by di()

    override val duplicates = settableList<GameDuplicates>()
//    override val excludeGameActions = channel<Game>()

    override val viewGameDetailsActions = channel<ViewGameParams>()

    override val searchText = userMutableState("")
    override val matchingGame = state<Game?>(null)

    override val hideViewActions = channel<Unit>()
    override val customNavigationButton = backButton { action(hideViewActions) }

    private val gamesView = prettyListView(duplicates) {
        prettyListCell { duplicate ->
            text = null
            graphic = GameDetailsSummaryBuilder(duplicate.game).build()
        }

        gameContextMenu.install(this) { ViewGameParams(selectionModel.selectedItem.game, duplicates.map { it.game }) }
        onUserSelect { viewGameDetailsActions.event(ViewGameParams(selectionModel.selectedItem.game, duplicates.map { it.game })) }
    }

    private val selectedDuplicate = gamesView.selectionModel.selectedItemProperty()
    private val duplicatesOfSelectedGame = selectedDuplicate.mapToList { it?.duplicates ?: emptyList() }

    private val duplicatesView = prettyListView(duplicatesOfSelectedGame) {
        prettyListCell { duplicate ->
            text = null
            graphic = HBox().apply {
                spacing = 20.0
                addClass(Style.duplicateItem)
                stackpane {
                    minWidth = 160.0
                    alignment = Pos.CENTER
                    children += commonOps.providerLogo(duplicate.providerId).toImageView(height = 80.0, width = 160.0)
                }
                vbox {
                    header(duplicate.game.name) {
                        useMaxSize = true
                        hgrow = Priority.ALWAYS
                    }
                    spacer()
                    label(duplicate.game.path.toString(), Icons.folder.size(20))
                }
            }
        }
        selectionModel.selectedItemProperty().typeSafeOnChange {
            if (it != null) {
                selectGame(it.game)
            }
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
            add(duplicatesView.apply { vgrow = Priority.ALWAYS })
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
        gamesView.selectionModel.select(duplicates.indexOfFirst { it.game.id == game.id })
    }

    override fun HBox.buildToolbar() {
        searchTextField(this@JavaFxDuplicatesScreen, searchText.property) { isFocusTraversable = false }
//        gap()
//        excludeButton {
//            action(excludeGameActions) { selectedDuplicate.value!!.game }
//            tooltip(selectedDuplicate.stringBinding { duplicate ->
//                "Exclude game '${duplicate?.game?.name}' from duplicates report."
//            })
//        }
        spacer()
        header(duplicates.sizeProperty.stringBinding { "Duplicates: $it" })
        gap()
    }

    class Style : Stylesheet() {
        companion object {
            val duplicateItem by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            duplicateItem {
                fontSize = 18.px
            }
        }
    }
}
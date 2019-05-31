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

package com.gitlab.ykrasik.gamedex.app.javafx.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanOpenFile
import com.gitlab.ykrasik.gamedex.app.api.game.ViewGameParams
import com.gitlab.ykrasik.gamedex.app.api.report.*
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.game.details.GameDetailsSummaryBuilder
import com.gitlab.ykrasik.gamedex.app.javafx.game.details.JavaFxGameDetailsView
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.header
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 16:25
 */
class JavaFxReportScreen : PresentableScreen("Reports", Icons.chart),
    ReportView,
    ViewCanExcludeGameFromReport,
    ViewCanSearchReportResult,
    ViewCanAddOrEditReport,
    ViewCanOpenFile {

    private val gameDetailsView = JavaFxGameDetailsView(
        canClose = false,
        imageFitWidth = screenBounds.width / 3,
        imageFitHeight = screenBounds.height * 2 / 3, // TODO: This sucks, find a way to make this be dynamic.
        maxDetailsWidth = screenBounds.width / 4
    )

    override val excludeGameActions = channel<Game>()

    override val addOrEditReportActions = channel<Report?>()

    override val searchText = userMutableState("")

    override val matchingGame = state<Game?>(null)

    override val openFileActions = channel<File>()

    override val report = userMutableState(Report.Null)
    override val result = state(ReportResult.Null)
    private val games = result.property.mapToList { it.games }

    private val gamesView = prettyListView(games) {
        prefWidth = 900.0
        maxWidth = prefWidth
        prettyListCell { game ->
            text = null
            graphic = GameDetailsSummaryBuilder(game) {
                nameOp = { maxWidth = 550.0 }
                pathOp = { maxWidth = 550.0 }
            }.build()
        }
    }

    override val root = hbox {
        // Left
        vbox {
            maxWidth = screenBounds.width / 2
            add(gamesView.apply { vgrow = Priority.ALWAYS })
        }

        // Right
        addComponent(gameDetailsView) {
            root.hgrow = Priority.ALWAYS
            gamesView.selectionModel.selectedItemProperty().typeSafeOnChange {
                if (it != null) {
                    gameParams.valueFromView = ViewGameParams(it, listOf(it))
                }
            }
        }
    }

    init {
        register()

        matchingGame.property.typeSafeOnChange { match ->
            if (match != null) {
                gamesView.selectionModel.select(match)
            }
        }
    }

    override fun HBox.buildToolbar() {
        jfxButton {
            textProperty().bind(report.property.stringBinding { it!!.name })
            graphicProperty().bind(hoverProperty().binding { if (it) Icons.edit else Icons.chart })
            action(addOrEditReportActions) { report.value }
        }
        gap()
        header(games.sizeProperty.stringBinding { "Games: $it" })
        gap()
        searchTextField(this@JavaFxReportScreen, searchText.property) { isFocusTraversable = false }
    }
}
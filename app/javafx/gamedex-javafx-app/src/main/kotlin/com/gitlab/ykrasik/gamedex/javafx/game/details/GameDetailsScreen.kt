/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.javafx.game.details

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.game.GameController
import com.gitlab.ykrasik.gamedex.app.javafx.game.discover.discoverGameChooseResultsMenu
import com.gitlab.ykrasik.gamedex.javafx.image.JavaFxImageRepository
import com.gitlab.ykrasik.gamedex.javafx.screen.GamedexScreen
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ToolBar
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 30/03/2017
 * Time: 18:17
 */
class GameDetailsScreen : GamedexScreen("Details", icon = null) {
    private val gameController: GameController by di()
    private val imageRepository: JavaFxImageRepository by di()

    private val browser = YouTubeWebBrowser()

    val gameProperty: ObjectProperty<Game> = SimpleObjectProperty()
    var game by gameProperty

    override val useDefaultNavigationButton = false

    override fun ToolBar.constructToolbar() {
        editButton { setOnAction { editDetails() } }
        verticalSeparator()
        tagButton { setOnAction { tag() } }
        verticalSeparator()

        spacer()

        verticalSeparator()
        searchButton("Re-Discover") {
            enableWhen { gameController.canRunLongTask }
            dropDownMenu(PopOver.ArrowLocation.RIGHT_TOP, closeOnClick = false) {
                discoverGameChooseResultsMenu()
            }
            setOnAction { reloadGame { gameController.searchGame(game) } }
        }
        verticalSeparator()
        refreshButton {
            enableWhen { gameController.canRunLongTask }
            setOnAction { reloadGame { gameController.refreshGame(game) } }
        }
        verticalSeparator()
        deleteButton("Delete") {
            setOnAction {
                javaFx {
                    if (gameController.delete(game)) {
                        goBackScreen()
                    }
                }
            }
        }
        verticalSeparator()
    }

    override val root = hbox {
        setId(Style.gameDetailsViewContent)

        // Left
        stackpane {
            setId(Style.leftGameDetailsView)
            addClass(CommonStyle.card)

            val poster = ImageView()
            val gamePosterProperty = gameProperty.flatMap { game ->
                // TODO: Make persistIfAbsent a configuration value.
                imageRepository.fetchImage(game?.posterUrl, game?.id ?: -1, persistIfAbsent = false)
            }
            poster.imageProperty().bind(gamePosterProperty)

            contextmenu {
                item("Change", graphic = Theme.Icon.poster(20.0)).action { editDetails(GameDataType.poster) }
            }

            imageViewResizingPane(poster) {
                maxWidth = com.gitlab.ykrasik.gamedex.javafx.screenBounds.width * maxPosterWidthPercent

                // Clip the posterPane's corners to be round after the posterPane's size is calculated.
                clipRectangle {
                    arcWidth = 20.0
                    arcHeight = 20.0
                    heightProperty().bind(this@imageViewResizingPane.heightProperty())
                    widthProperty().bind(this@imageViewResizingPane.widthProperty())
                }
            }
        }

        region { setId(Style.middleGameDetailsView) }

        // Right
        vbox {
            setId(Style.rightGameDetailsView)
            addClass(CommonStyle.card)
            hgrow = Priority.ALWAYS

            // Top
            stackpane {
                gameProperty.perform { game ->
                    if (game != null) {
                        replaceChildren {
                            children += GameDetailsFragment(game, evenIfEmpty = true).root
                        }
                    }
                }
            }

            separator { paddingTop = 10.0 }

            // Bottom
            children += browser.root.apply { vgrow = Priority.ALWAYS }
            gameProperty.perform { game ->
                if (game != null) browser.searchYoutube(game)
            }
        }
    }

    init {
        titleProperty.bind(gameProperty.map { it?.name })
    }

    override fun onUndock() {
        browser.stop()
    }

    private fun editDetails(type: GameDataType = GameDataType.name_) = reloadGame {
        gameController.editDetails(game, initialTab = type)
    }

    private fun tag() = reloadGame { gameController.tag(game) }

    private fun reloadGame(f: suspend () -> Game) {
        javaFx {
            game = f()
        }
    }

    private fun goBackScreen() {
        closeRequestedProperty.value = true
    }

    companion object {
        private val maxPosterWidthPercent = 0.44
    }

    class Style : Stylesheet() {
        companion object {
            val gameDetailsViewContent by cssid()
            val leftGameDetailsView by cssid()
            val middleGameDetailsView by cssid()
            val rightGameDetailsView by cssid()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            gameDetailsViewContent {
                padding = box(2.px)
            }

            leftGameDetailsView {
            }

            middleGameDetailsView {
                padding = box(2.px)
            }

            rightGameDetailsView {
                padding = box(5.px)
            }
        }
    }
}
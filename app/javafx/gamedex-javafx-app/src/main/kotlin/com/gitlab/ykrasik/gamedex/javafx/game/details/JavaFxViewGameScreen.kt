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
import com.gitlab.ykrasik.gamedex.app.api.game.*
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.api.web.ViewWithBrowser
import com.gitlab.ykrasik.gamedex.app.javafx.game.discover.discoverGameChooseResultsMenu
import com.gitlab.ykrasik.gamedex.app.javafx.image.ImageLoader
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import com.gitlab.ykrasik.gamedex.javafx.view.WebBrowser
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ToolBar
import javafx.scene.layout.Priority
import kotlinx.coroutines.experimental.Deferred
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 30/03/2017
 * Time: 18:17
 */
class JavaFxViewGameScreen : PresentableScreen(), GameView, ViewCanEditGame, ViewCanDeleteGame,
    ViewCanTagGame, ViewCanRediscoverGame, ViewCanRedownloadGame, ViewWithBrowser {
    private val imageLoader: ImageLoader by di()

    private val browser = WebBrowser()

    override val gameChanges = channel<Game?>()
    private val gameProperty = SimpleObjectProperty<Game>().eventOnChange(gameChanges)
    override var game by gameProperty

    private val posterProperty = SimpleObjectProperty<Deferred<Image>?>(null)
    override var poster by posterProperty

    override val editGameActions = channel<Pair<Game, GameDataType>>()
    override val deleteGameActions = channel<Game>()
    override val tagGameActions = channel<Game>()
    override val redownloadGameActions = channel<Game>()
    override val rediscoverGameActions = channel<Game>()

    private val gameDetailsView = JavaFxGameDetailsView(evenIfEmpty = true)

    init {
        viewRegistry.register(this)
    }

    override fun ToolBar.constructToolbar() {
        editButton { setOnAction { editGame(GameDataType.name_) } }
        verticalSeparator()
        tagButton { eventOnAction(tagGameActions) { game } }
        verticalSeparator()

        spacer()

        verticalSeparator()
        searchButton("Re-Discover") {
            dropDownMenu(PopOver.ArrowLocation.RIGHT_TOP, closeOnClick = false) {
                discoverGameChooseResultsMenu()
            }
            eventOnAction(rediscoverGameActions) { game }
        }
        verticalSeparator()
        downloadButton("Re-Download") { eventOnAction(redownloadGameActions) { game } }
        verticalSeparator()
        deleteButton("Delete") { eventOnAction(deleteGameActions) { game } }
        verticalSeparator()
    }

    override val root = hbox {
        setId(Style.gameDetailsViewContent)

        // Left
        stackpane {
            setId(Style.leftGameDetailsView)
            addClass(CommonStyle.card)

            contextmenu {
                item("Change", graphic = Theme.Icon.poster(20.0)).action {
                    editGame(GameDataType.poster)
                }
            }

            imageViewResizingPane(posterProperty.flatMap { imageLoader.loadImage(it) }) {
                maxWidth = screenBounds.width * maxPosterWidthPercent

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
            addComponent(gameDetailsView)
            gameProperty.onChange {
                gameDetailsView.game = it
            }

            separator { paddingTop = 10.0 }

            // Bottom
            addComponent(browser)
        }
    }

    private fun editGame(initialTab: GameDataType) = editGameActions.event(game to initialTab)

    override fun browseTo(url: String?) = browser.load(url)

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
                importStylesheetSafe(Style::class)
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
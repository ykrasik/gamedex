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
import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.game.common.ViewCanDeleteGame
import com.gitlab.ykrasik.gamedex.app.api.game.details.GameDetailsView
import com.gitlab.ykrasik.gamedex.app.api.game.discover.ViewCanDiscoverGamesWithoutProviders
import com.gitlab.ykrasik.gamedex.app.api.game.discover.ViewCanDiscoverNewGames
import com.gitlab.ykrasik.gamedex.app.api.game.discover.ViewCanRediscoverGame
import com.gitlab.ykrasik.gamedex.app.api.game.download.ViewCanRedownloadGame
import com.gitlab.ykrasik.gamedex.app.api.game.edit.ViewCanEditGame
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.presenters
import com.gitlab.ykrasik.gamedex.app.javafx.game.discover.discoverGameChooseResultsMenu
import com.gitlab.ykrasik.gamedex.app.javafx.game.edit.JavaFxEditGameView
import com.gitlab.ykrasik.gamedex.app.javafx.image.ImageLoader
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.game.common.DeleteGameView
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableGamedexScreen
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
class JavaFxGameDetailsScreen : PresentableGamedexScreen(),
    GameDetailsView, ViewCanEditGame, ViewCanDeleteGame, ViewCanDiscoverNewGames, ViewCanDiscoverGamesWithoutProviders,
    ViewCanRediscoverGame, ViewCanRedownloadGame {
    private val viewManager: ViewManager by di()
    private val imageLoader: ImageLoader by di()

    private val editGameView: JavaFxEditGameView by inject()

    private val browser = YouTubeWebBrowser()

    private val gameProperty = SimpleObjectProperty<Game>()
    override var game by gameProperty

    private val posterProperty = SimpleObjectProperty<Deferred<Image>?>(null)
    override var poster by posterProperty

    override val useDefaultNavigationButton = false

    private val gameDetailsPresenter = presenters.gameDetails.present(this)
    private val editGamePresenter = presenters.editGame.present(this)
    private val rediscoverGamePresenter = presenters.rediscoverGame.present(this)
    private val redownloadGamePresenter = presenters.redownloadGame.present(this)
    private val deleteGamePresenter = presenters.deleteGame.present(this)

    override fun ToolBar.constructToolbar() {
        editButton { onAction { editGame(GameDataType.name_) } }
        verticalSeparator()
        tagButton { onAction { viewManager.showTagGameView(game) } }
        verticalSeparator()

        spacer()

        verticalSeparator()
        searchButton("Re-Discover") {
            dropDownMenu(PopOver.ArrowLocation.RIGHT_TOP, closeOnClick = false) {
                discoverGameChooseResultsMenu()
            }
            onAction { rediscoverGamePresenter.rediscoverGame(game) }
        }
        verticalSeparator()
        downloadButton("Re-Download") { onAction { redownloadGamePresenter.redownloadGame(game) } }
        verticalSeparator()
        deleteButton("Delete") { onAction { deleteGamePresenter.deleteGame(game) } }
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
            stackpane {
                gameProperty.onChange { game ->
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
        }
    }

    override fun onUndock() {
        browser.stop()
    }

    fun show(game: Game) = gameDetailsPresenter.onShow(game)

    override fun displayWebPage(url: String) = browser.load(url)

    override fun showEditGameView(game: Game, initialTab: GameDataType) = editGameView.show(game, initialTab)

    override fun showConfirmDeleteGame(game: Game) = DeleteGameView.showConfirmDeleteGame(game)

    private fun editGame(initialTab: GameDataType) = editGamePresenter.editGame(game, initialTab)

    override fun requestClose() {
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
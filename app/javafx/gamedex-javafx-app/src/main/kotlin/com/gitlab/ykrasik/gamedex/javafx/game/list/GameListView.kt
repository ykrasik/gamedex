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

package com.gitlab.ykrasik.gamedex.javafx.game.list

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.javafx.game.GameController
import com.gitlab.ykrasik.gamedex.javafx.image.JavaFxImageRepository
import com.gitlab.ykrasik.gamedex.javafx.imageview
import com.gitlab.ykrasik.gamedex.javafx.perform
import com.gitlab.ykrasik.gamedex.javafx.screenBounds
import com.gitlab.ykrasik.gamedex.javafx.toDisplayString
import com.gitlab.ykrasik.gamedex.javafx.game.details.GameDetailsFragment
import com.gitlab.ykrasik.gamedex.javafx.game.menu.GameContextMenu
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.scene.control.ListView
import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 15:06
 */
class GameListView : View("Game List") {
    private val gameController: GameController by di()
    private val imageRepository: JavaFxImageRepository by di()

    private val gameContextMenu: GameContextMenu by di()

    private var listview: ListView<Game> by singleAssign()

    override val root = borderpane {
        center {
            borderpane {
                top {
                    gridpane {
                        hgap = 10.0
                        row {
                            label("Thumbnail") { addClass(Style.gameItemHeader, Style.thumbnailHeader) }
                            label("Name") { addClass(Style.gameItemHeader, Style.nameHeader) }
                            label("Release Date") { addClass(Style.gameItemHeader, Style.releaseDateHeader) }
                            label("Critic Score") { addClass(Style.gameItemHeader, Style.criticScoreHeader) }
                            label("User Score") { addClass(Style.gameItemHeader, Style.userScoreHeader) }
                        }
                    }
                }
                center {
                    listview = listview(gameController.sortedFilteredGames) {
                        minHeight = screenBounds.height
                        cellFormat { game ->
                            graphic = gridpane {
                                hgap = 10.0
                                row {
                                    stackpane {
                                        addClass(Style.gameItemValue, Style.thumbnailHeader, Style.thumbnailValue)
                                        imageview(imageRepository.fetchImage(game.imageUrls.thumbnailUrl, game.id, persistIfAbsent = true)) {
                                            fitHeight = 75.0
                                            fitWidth = 75.0
                                            isPreserveRatio = true
                                        }
                                    }
                                    label(game.name) { addClass(Style.gameItemValue, Style.nameHeader, Style.nameValue) }
                                    label(game.releaseDate.toDisplayString()) { addClass(Style.gameItemValue, Style.releaseDateHeader, Style.releaseDateValue) }
                                    // TODO: Displays score wrong.
                                    label(game.criticScore.toDisplayString()) { addClass(Style.gameItemValue, Style.criticScoreHeader, Style.criticScoreValue) }
                                    label(game.userScore.toDisplayString()) { addClass(Style.gameItemValue, Style.userScoreHeader, Style.userScoreValue) }
                                }
                            }
                            setOnMouseClicked { e ->
                                if (e.clickCount == 2) {
                                    gameController.viewDetails(game)
                                }
                            }
                        }

                        gameContextMenu.install(this) { selectedItem!! }
                    }
                }
            }
        }
        val selectedGame = listview.selectionModel.selectedItemProperty()

        right {
            stackpane {
                setId(Style.sideView)
                gridpane {
                    alignment = Pos.TOP_CENTER
                    selectedGame.perform { game ->
                        if (game != null) {
                            replaceChildren {
                                row {
                                    imageview(imageRepository.fetchImage(game.imageUrls.posterUrl, game.id, persistIfAbsent = false)) {
                                        gridpaneConstraints { hAlignment = HPos.CENTER }
                                        isPreserveRatio = true
                                        fitHeight = 650.0
                                        fitWidth = 650.0
                                    }
                                }
                                row {
                                    gridpaneConstraints { hAlignment = HPos.CENTER }
                                    children += GameDetailsFragment(game).root
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    class Style : Stylesheet() {
        companion object {
            val gameItemHeader by cssclass()
            val gameItemValue by cssclass()

            val thumbnailHeader by cssclass()
            val thumbnailValue by cssclass()

            val nameHeader by cssclass()
            val nameValue by cssclass()

            val releaseDateHeader by cssclass()
            val releaseDateValue by cssclass()

            val criticScoreHeader by cssclass()
            val criticScoreValue by cssclass()

            val userScoreHeader by cssclass()
            val userScoreValue by cssclass()

            val sideView by cssid()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            gameItemHeader {
                alignment = Pos.CENTER_LEFT
                padding = box(vertical = 3.px, horizontal = 3.px)
            }

            gameItemValue {
                minHeight = 80.px
                maxHeight = 80.px
                alignment = Pos.CENTER_LEFT
                padding = box(vertical = 3.px, horizontal = 5.px)
                backgroundRadius = multi(box(5.px))
            }

            thumbnailHeader {
                minWidth = 85.px
                maxWidth = 85.px
            }

            thumbnailValue {
                alignment = Pos.BASELINE_CENTER
            }

            nameHeader {
                minWidth = 600.px
                maxWidth = 600.px
            }

            nameValue {
                backgroundColor = multi(Color.LIGHTBLUE)
            }

            releaseDateHeader {
                minWidth = 100.px
                maxWidth = 100.px
            }

            releaseDateValue {
                alignment = Pos.BASELINE_CENTER
                backgroundColor = multi(Color.LIMEGREEN)
            }

            criticScoreHeader {
                minWidth = 150.px
                maxWidth = 150.px
            }

            criticScoreValue {
                alignment = Pos.BASELINE_CENTER
                backgroundColor = multi(Color.DARKORANGE)
            }

            userScoreHeader {
                minWidth = 150.px
                maxWidth = 150.px
            }

            userScoreValue {
                alignment = Pos.BASELINE_CENTER
                backgroundColor = multi(Color.DARKGOLDENROD)
            }

            sideView {
                minWidth = 750.px
                maxWidth = 750.px
                padding = box(20.px)
                fillWidth = true
            }
        }
    }
}
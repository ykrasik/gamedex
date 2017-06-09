package com.gitlab.ykrasik.gamedex.ui.view.game.list

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.ui.imageview
import com.gitlab.ykrasik.gamedex.ui.perform
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle.Companion.toDisplayString
import com.gitlab.ykrasik.gamedex.ui.view.game.GameScreen.Companion.gameContextMenu
import com.gitlab.ykrasik.gamedex.ui.view.game.details.GameDetailsFragment
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.scene.control.ListView
import javafx.scene.paint.Color
import javafx.stage.Screen
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 15:06
 */
class GameListView : View("Game List") {
    private val gameController: GameController by di()
    private val imageLoader: ImageLoader by di()

    private var listview: ListView<Game> by singleAssign()

    // TODO: This should probably be a master-detail pane
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
                        minHeight = Screen.getPrimary().bounds.height
                        cellFormat { game ->
                            graphic = gridpane {
                                hgap = 10.0
                                row {
                                    stackpane {
                                        addClass(Style.gameItemValue, Style.thumbnailHeader, Style.thumbnailValue)
                                        imageview(imageLoader.fetchImage(game.id, game.imageUrls.thumbnailUrl, persistIfAbsent = true)) {
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

                        gameContextMenu(gameController) { selectedItem!! }
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
                                    imageview(imageLoader.fetchImage(game.id, game.imageUrls.posterUrl, persistIfAbsent = true)) {
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
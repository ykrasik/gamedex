package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.widgets.GameDetailSnippetFactory
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.Button
import javafx.scene.effect.BlurType
import javafx.scene.effect.DropShadow
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import javafx.scene.web.WebView
import javafx.stage.Screen
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.*
import java.net.URLEncoder

/**
 * User: ykrasik
 * Date: 30/03/2017
 * Time: 18:17
 */
// TODO: This class is too big
class GameDetailsFragment(initialGame: Game, displayVideos: Boolean = true) : Fragment() {
    private val gameController: GameController by di()
    private val imageLoader: ImageLoader by di()
    private val detailsSnippetFactory: GameDetailSnippetFactory by di()

    private var webView: WebView by singleAssign()
    private var backButton: Button by singleAssign()
    private var forwardButton: Button by singleAssign()

    private val gameProperty: ObjectProperty<Game> = SimpleObjectProperty(initialGame)
    private var game by gameProperty

    private var accept = false

    override val root = borderpane {
        setId(Style.gameDetailsView)
        top {
            toolbar {
                acceptButton { setOnAction { close(accept = true) } }

                verticalSeparator()

                jfxButton("Refresh", graphic = FontAwesome.Glyph.REFRESH.toGraphic { size(22.0); color(Color.DARKCYAN) }) {
                    addClass(CommonStyle.toolbarButton)
                    setOnAction {
                        val task = gameController.refreshGame(game)
                        disableWhen { task.runningProperty }
                        reloadGame { task.result }
                    }
                }

                verticalSeparator()

                jfxButton("Rediscover", graphic = FontAwesome.Glyph.SEARCH.toGraphic { size(22.0); color(Color.DARKGOLDENROD) }) {
                    addClass(CommonStyle.toolbarButton)
                    setOnAction {
                        val task = gameController.rediscoverGame(game)
                        disableWhen { task.runningProperty }
                        reloadGame { task.result }
                    }
                }

                verticalSeparator()

                jfxButton("Edit", graphic = FontAwesome.Glyph.PENCIL.toGraphic { size(22.0); color(Color.ORANGE) }) {
                    addClass(CommonStyle.toolbarButton)
                    setOnAction { editDetails() }
                }

                verticalSeparator()

                jfxButton("Tag", graphic = FontAwesome.Glyph.TAG.toGraphic { size(22.0); color(Color.BLUEVIOLET) }) {
                    addClass(CommonStyle.toolbarButton)
                    setOnAction { tag() }
                }

                verticalSeparator()

                spacer()

                verticalSeparator()

                deleteButton {
                    setOnAction {
                        if (gameController.delete(game)) {
                            close(accept = false)
                        }
                    }
                }

                verticalSeparator()
            }
        }
        center {
            val screenWidth = Screen.getPrimary().bounds.width
            hbox {
                setId(Style.gameDetailsViewContent)

                // Left
                stackpane {
                    setId(Style.leftGameDetailsView)
                    addClass(CommonStyle.card)

                    val poster = ImageView()
                    val gamePosterProperty = gameProperty.flatMap { game ->
                        imageLoader.fetchImage(game!!.id, game.posterUrl, persistIfAbsent = true)
                    }
                    poster.imageProperty().bind(gamePosterProperty)

                    contextmenu {
                        menuitem("Change", graphic = FontAwesome.Glyph.PICTURE_ALT.toGraphic()) { editDetails(GameDataType.poster) }
                    }

                    imageViewResizingPane(poster) {
                        maxWidth = screenWidth * maxPosterWidthPercent

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

                // TODO: See if this can be made collapsible - squeezebox?
                // Right
                vbox {
                    setId(Style.rightGameDetailsView)
                    addClass(CommonStyle.card)
                    hgrow = Priority.ALWAYS

                    // Top
                    stackpane {
                        gameProperty.perform { game ->
                            replaceChildren {
                                children += detailsSnippetFactory.create(
                                    game,
                                    onGenrePressed = this@GameDetailsFragment::onGenrePressed,
                                    onTagPressed = this@GameDetailsFragment::onTagPressed
                                )
                            }
                        }
                    }

                    // Bottom
                    separator { padding { top = 10; bottom = 10 } }
                    gridpane {
                        padding { top = 5; bottom = 5 }
                        hgap = 10.0
                        row {
                            backButton = button(graphic = FontAwesome.Glyph.ARROW_LEFT.toGraphic())
                            forwardButton = button(graphic = FontAwesome.Glyph.ARROW_RIGHT.toGraphic())
                        }
                    }
                    webView = webview {
                        fun canNavigate(back: Boolean): Property<Boolean> {
                            val history = engine.history
                            val entries = history.entries
                            return history.currentIndexProperty().map { i ->
                                val currentIndex = i!!.toInt()
                                entries.size > 1 && (if (back) currentIndex > 0 else currentIndex < entries.size - 1)
                            }
                        }

                        fun navigate(back: Boolean) = engine.history.go(if (back) -1 else 1)

                        with(backButton) {
                            enableWhen { canNavigate(back = true) }
                            setOnAction { navigate(back = true) }
                        }
                        with(forwardButton) {
                            enableWhen { canNavigate(back = false) }
                            setOnAction { navigate(back = false) }
                        }
                        if (displayVideos) {
                            vgrow = Priority.ALWAYS
                            val search = URLEncoder.encode("${game.name} ${game.platform} gameplay", "utf-8")
                            val url = "https://www.youtube.com/results?search_query=$search"
                            engine.load(url)
                        }
                    }
                }
            }
        }
    }

    init {
        titleProperty.bind(gameProperty.map { it!!.name })
    }

    override fun onDock() {
        modalStage!!.isMaximized = true
    }

    override fun onUndock() {
        webView.engine.load(null)
    }

    fun show(): Boolean {
        openModal(block = true, owner = null)
        return accept
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    private fun onGenrePressed(genre: String) {
        gameController.sortedFilteredGames.genreFilter = genre
        close(accept = false)
    }

    private fun onTagPressed(tag: String) {
        gameController.sortedFilteredGames.tagFilter = tag
        close(accept = false)
    }

    private fun editDetails(type: GameDataType = GameDataType.name_) = reloadGame {
        gameController.editDetails(game, initialTab = type)
    }

    private fun tag() = reloadGame { gameController.tag(game) }

    private fun reloadGame(f: () -> Deferred<Game>) {
        val newGame = f()
        launch(JavaFx) {
            game = newGame.await()
        }
    }

    companion object {
        private val maxPosterWidthPercent = 0.44
    }

    class Style : Stylesheet() {
        companion object {
            val gameDetailsView by cssid()
            val gameDetailsViewContent by cssid()
            val leftGameDetailsView by cssid()
            val middleGameDetailsView by cssid()
            val rightGameDetailsView by cssid()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            gameDetailsView {
                backgroundColor = multi(
                    LinearGradient(0.0, 1.0, 0.0, 1.0, false, CycleMethod.NO_CYCLE,
                        Stop(0.0, Color.web("#f2f2f2")), Stop(1.0, Color.web("#d6d6d6"))),
                    LinearGradient(0.0, 1.0, 0.0, 1.0, false, CycleMethod.NO_CYCLE,
                        Stop(0.0, Color.web("#fcfcfc")), Stop(0.2, Color.web("#d9d9d9")), Stop(1.0, Color.web("#d6d6d6"))),
                    LinearGradient(0.0, 1.0, 0.0, 1.0, false, CycleMethod.NO_CYCLE,
                        Stop(0.0, Color.web("#dddddd")), Stop(0.5, Color.web("#f6f6f6")))
                )
                backgroundRadius = multi(box(8.px, 7.px, 6.px, 0.px))
                backgroundInsets = multi(box(0.px, 1.px, 2.px, 0.px))
                textFill = Color.BLACK
                effect = DropShadow(BlurType.THREE_PASS_BOX, Color.rgb(0, 0, 0, 0.6), 5.0, 0.0, 0.0, 1.0)
            }

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
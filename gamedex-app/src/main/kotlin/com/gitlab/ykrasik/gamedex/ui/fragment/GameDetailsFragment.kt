package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.widgets.GameDetailSnippetFactory
import javafx.beans.property.Property
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
import kotlinx.coroutines.experimental.Job
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.*
import java.net.URLEncoder

/**
 * User: ykrasik
 * Date: 30/03/2017
 * Time: 18:17
 */
// TODO: This needs to refresh itself when data is updated.
// TODO: This class is already too big
class GameDetailsFragment(private val game: Game, displayVideos: Boolean = true) : Fragment(game.name) {
    private val gameController: GameController by di()
    private val imageLoader: ImageLoader by di()
    private val detailsSnippetFactory: GameDetailSnippetFactory by di()

    private var webView: WebView by singleAssign()
    private var backButton: Button by singleAssign()
    private var forwardButton: Button by singleAssign()

    private var accept = false

    override val root = borderpane {
        setId(Style.gameDetailsView)
        top {
            toolbar {
                acceptButton {
                    addClass(CommonStyle.toolbarButton)
                    setOnAction { close(accept = true) }
                }

                verticalSeparator()

                jfxButton("Refresh", graphic = FontAwesome.Glyph.REFRESH.toGraphic { size(22.0); color(Color.BLUE) }) {
                    addClass(CommonStyle.toolbarButton)
                    setOnAction {
                        val task = gameController.refreshGame(game)
                        disableProperty().cleanBind(task.runningProperty)
                    }
                }

                verticalSeparator()

                spacer()

                verticalSeparator()

                extraMenu {
                    extraMenuItem("Rediscover", graphic = FontAwesome.Glyph.SEARCH.toGraphic()) {
                        val task = gameController.rediscoverGame(game)
                        disableProperty().cleanBind(task.runningProperty)
                    }

                    separator()

                    extraMenuItem("Change Thumbnail", graphic = FontAwesome.Glyph.FILE_IMAGE_ALT.toGraphic()) {
                        changeThumbnail()
                    }

                    extraMenuItem("Change Poster", graphic = FontAwesome.Glyph.PICTURE_ALT.toGraphic()) {
                        changePoster()
                    }

                    separator()

                    extraMenuItem("Delete", graphic = FontAwesome.Glyph.TRASH.toGraphic(), op = { addClass(CommonStyle.deleteButton) }) {
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
                    poster.imageProperty().bind(imageLoader.fetchImage(game.id, game.posterUrl, persistIfAbsent = true))

                    contextmenu {
                        menuitem("Change", graphic = FontAwesome.Glyph.PICTURE_ALT.toGraphic()) { changePoster() }
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
                children += detailsSnippetFactory.create(game, onGenrePressed = this@GameDetailsFragment::onGenrePressed).apply {
                    setId(Style.rightGameDetailsView)
                    addClass(CommonStyle.card)
                    hgrow = Priority.ALWAYS

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
                            return history.currentIndexProperty().mapProperty { i ->
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
        gameController.sortedFilteredGames.genreFilterProperty.set(genre)
        close(accept = false)
    }

    private fun changePoster() = hideTemporarily {
        gameController.changePoster(game)
    }

    private fun changeThumbnail() = hideTemporarily {
        gameController.changeThumbnail(game)
    }

    private fun hideTemporarily(f: () -> Job) {
        close(accept = false)
        f().invokeOnCompletion {
            show()
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
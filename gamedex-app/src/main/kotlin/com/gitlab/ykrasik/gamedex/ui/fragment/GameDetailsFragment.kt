package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.view.Styles
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
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.*
import java.net.URLEncoder

/**
 * User: ykrasik
 * Date: 30/03/2017
 * Time: 18:17
 */
// TODO: This will need to learn to refresh itself when data is updated.
// TODO: This class is already too big
class GameDetailsFragment(game: Game, displayVideos: Boolean = true) : Fragment(game.name) {
    private val gameController: GameController by di()
    private val imageLoader: ImageLoader by di()
    private val detailsSnippetFactory: GameDetailSnippetFactory by di()

    private var webView: WebView by singleAssign()
    private var backButton: Button by singleAssign()
    private var forwardButton: Button by singleAssign()

    private var accept = false

    override val root = borderpane {
        setId(Style.gameDetailView)
        top {
            vbox {
                buttonbar {
                    padding { right = 10; left = 10 }
                    minHeight = 40.0
                    okButton { setOnAction { close(accept = true) } }
                    cancelButton { setOnAction { close(accept = false) } }

                    button("Change Thumbnail") { setOnAction { gameController.changeThumbnail(game) } }
                    // TODO: Implement.
                    button("Change Poster")
                    button("Refresh")
                    button("Search Again") { setOnAction { gameController.searchAgain(game) } }
                    button("Delete") { setOnAction { if (gameController.delete(game)) { close(accept = false) } } }
                }
                separator()
            }
        }
        center {
            paddingAll = 10
            val screenWidth = Screen.getPrimary().bounds.width
            hbox {
                // Left
                stackpane {
                    addClass(Styles.card)       // TODO: Not sure what this does

                    val poster = ImageView()
                    poster.imageProperty().bind(imageLoader.fetchImage(game.id, game.posterUrl, persistIfAbsent = true))

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

                verticalSeparator()

                // TODO: Check if the new form can do what we did here manually.
                // TODO: See if this can be made collapsible - squeezebox?
                // Right
                children += detailsSnippetFactory.create(game).apply {
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

    private fun WebView.canNavigate(back: Boolean): Property<Boolean> {
        val history = engine.history
        val entries = history.entries
        return history.currentIndexProperty().mapProperty { i ->
            val currentIndex = i!!.toInt()
            entries.size > 1 && (if (back) currentIndex > 0 else currentIndex < entries.size - 1)
        }
    }

    private fun WebView.navigate(back: Boolean) {
        engine.history.go(if (back) -1 else 1)
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

    companion object {
        private val maxPosterWidthPercent = 0.44
    }

    class Style : Stylesheet() {
        companion object {
            val gameDetailView by cssid()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            gameDetailView {
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
        }
    }
}
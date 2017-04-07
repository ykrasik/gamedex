package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.common.datamodel.Game
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.model.LibraryRepository
import com.gitlab.ykrasik.gamedex.ui.view.Styles
import javafx.scene.effect.BlurType
import javafx.scene.effect.DropShadow
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import javafx.scene.shape.Rectangle
import javafx.stage.Screen
import tornadofx.*
import java.net.URLEncoder

/**
 * User: ykrasik
 * Date: 30/03/2017
 * Time: 18:17
 */
class GameDetailsFragment(game: Game, displayVideos: Boolean = true) : Fragment(game.name) {
    private val imageLoader: ImageLoader by di()
    private val libraryRepository: LibraryRepository by di()

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

                    // TODO: Implement.
                    button("Change Thumbnail")
                    button("Change Poster")
                    button("Refresh")
                    button("Search Again")
                    button("Delete")
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
                    poster.imageProperty().bind(imageLoader.fetchImage(game.imageIds.posterId))

                    imageViewResizingPane(poster) {
                        maxWidth = screenWidth * maxPosterWidthPercent

                        // Clip the posterPane's corners to be round after the posterPane's size is calculated.
                        clip = Rectangle().apply {
                            arcWidth = 20.0
                            arcHeight = 20.0
                            heightProperty().bind(this@imageViewResizingPane.heightProperty())
                            widthProperty().bind(this@imageViewResizingPane.widthProperty())
                        }
                    }
                }

                verticalSeparator(padding = 10.0)

                // Right
                vbox {
                    hgrow = Priority.ALWAYS

                    // Top
                    hbox {
                        horizontalExpander()
                        label(game.name) { setId(Style.nameLabel) }
                        horizontalExpander()
                    }
                    form {
                        fieldset {
                            field("Path") { label (game.path.path) { addClass(Style.details) } }
                            // TODO: Long description doesn't wrap to next line.
                            field("Description") { label(game.description.toDisplayString()) { isWrapText = true; addClass(Style.details) } }
                            field("Release Date") { label(game.releaseDate.toDisplayString()) { addClass(Style.details) } }
                            field("Critic Score") {
                                gridpane {
                                    hgap = 5.0
                                    fixedRating(10) { rating = game.criticScore?.let { it / 10 } ?: 0.0 }
                                    label(game.criticScore.toDisplayString()) {
                                        addClass(Style.details)
                                        gridpaneConstraints { columnIndex = 1; }
                                    }
                                }
                            }
                            field("User Score") {
                                gridpane {
                                    hgap = 5.0
                                    fixedRating(10) { rating = game.userScore?.let { it / 10 } ?: 0.0 }
                                    label(game.userScore.toDisplayString()) {
                                        addClass(Style.details)
                                        gridpaneConstraints { columnIndex = 1; }
                                    }
                                }
                            }
                            field("Genres") {
                                game.genres.forEach {
                                    // TODO: Make clicking a genre change main view to only show these genres
                                    label(it) { addClass(Style.details, Style.genre) }
                                    paddingRight = 5.0
                                }
                            }
                            // TODO: Show url per provider.
//                            field("URL") { hyperlink(game.u) }
                        }
                    }

                    // Bottom
                    if (displayVideos) {
                        separator { padding { top = 10; bottom = 10 } }
                        webview {
                            vgrow = Priority.ALWAYS
                            val platform = libraryRepository.libraryForGame(game).platform
                            val search = URLEncoder.encode("${game.name} $platform gameplay", "utf-8")
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

    fun show(): Boolean {
        openModal(block = true, owner = null)
        return accept
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    private fun Any?.toDisplayString() = this?.toString() ?: "NA"

    companion object {
        private val maxPosterWidthPercent = 0.44
    }

    class Style : Stylesheet() {
        companion object {
            val gameDetailView by cssid()
            val details by cssclass()
            val nameLabel by cssid()
            val genre by cssclass()
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
                effect = DropShadow(BlurType.THREE_PASS_BOX, Color.rgb(0,0,0,0.6), 5.0, 0.0, 0.0, 1.0)
            }
            details {
                borderRadius = multi(box(3.px))
                backgroundColor = multi(Color.LIGHTGRAY)
                backgroundRadius = multi(box(3.px))
                padding = box(vertical = 5.px, horizontal = 10.px)
                fillWidth = true
                wrapText = true
            }

            nameLabel {
                fontSize = 20.px
            }

            genre {
                and(hover) {
                    translateX = 1.px
                    translateY = 1.px
                    effect = DropShadow(BlurType.GAUSSIAN, Color.web("#0093ff"), 12.0, 0.2, 0.0, 1.0)
                }
            }
        }
    }
}
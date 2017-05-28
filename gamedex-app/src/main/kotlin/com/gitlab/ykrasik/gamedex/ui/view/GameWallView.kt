package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.settings.GameWallSettings
import com.gitlab.ykrasik.gamedex.ui.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.fadeOnImageChange
import com.gitlab.ykrasik.gamedex.ui.popOver
import com.gitlab.ykrasik.gamedex.ui.view.GameView.Companion.gameContextMenu
import com.gitlab.ykrasik.gamedex.ui.widgets.GameDetailSnippetFactory
import com.gitlab.ykrasik.gamedex.ui.widgets.ImageViewLimitedPane
import javafx.css.StyleableObjectProperty
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Glow
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Screen
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 15:03
 */
// TODO: Allow "marking" a game with some marker - to delete, redownload etc.
class GameWallView : View("Games Wall") {
    private val gameController: GameController by di()
    private val settings: GameWallSettings by di()
    private val imageLoader: ImageLoader by di()
    private val gameDetailSnippetFactory: GameDetailSnippetFactory by di()

    override val root = datagrid(gameController.sortedFilteredGames.games) {
        cellHeightProperty.bind(settings.cellHeightProperty)
        cellWidthProperty.bind(settings.cellWidthProperty)
        (horizontalCellSpacingProperty as StyleableObjectProperty).bind(settings.cellHorizontalSpacingProperty)
        (verticalCellSpacingProperty as StyleableObjectProperty).bind(settings.cellVerticalSpacingProperty)

        val popOver = popOver()

        cellFactory = {
            val cell = GameWallCell()
            cell.setOnMouseClicked { e ->
                when (e.clickCount) {
                    1 -> with(popOver) {
                        if (isShowing) {
                            hide()
                        } else if (e.button == MouseButton.PRIMARY) {
                            arrowLocation = determineArrowLocation(e.screenX, e.screenY)
                            contentNode = gameDetailSnippetFactory.create(
                                cell.item!!,
                                withDescription = false,
                                withUrls = false,
                                onGenrePressed = { onGenrePressed(it) },
                                onTagPressed = { onTagPressed(it) }
                            ).apply {
                                addClass(Style.quickDetails)
                            }
                            show(cell)
                        }
                    }
                    2 -> {
                        popOver.hide()
                        if (e.button == MouseButton.PRIMARY) {
                            gameController.viewDetails(cell.item)
                        }
                    }
                }
            }
            cell.gameContextMenu(gameController) { cell.item }
            cell
        }
    }

    private fun PopOver.onGenrePressed(genre: String) {
        gameController.sortedFilteredGames.genreFilter = genre
        hide()
    }

    private fun PopOver.onTagPressed(tag: String) {
        gameController.sortedFilteredGames.tagFilter = tag
        hide()
    }

    private fun determineArrowLocation(x: Double, y: Double): PopOver.ArrowLocation {
        val screenBounds = Screen.getPrimary().bounds
        val maxX = screenBounds.maxX
        val maxY = screenBounds.maxY

        var arrowLocation = PopOver.ArrowLocation.TOP_LEFT
        if (x > maxX / 2) {
            arrowLocation = PopOver.ArrowLocation.TOP_RIGHT
        }
        if (y > maxY / 2) {
            arrowLocation = if (arrowLocation == PopOver.ArrowLocation.TOP_LEFT) {
                PopOver.ArrowLocation.BOTTOM_LEFT
            } else {
                PopOver.ArrowLocation.BOTTOM_RIGHT
            }
        }

        return arrowLocation
    }

    // TODO: Allow to overlay the library name as a ribbon over the image.
    // TODO: Consider adding an option to display the game name under the cell
    inner class GameWallCell : DataGridCell<Game>(root) {
        private val imageView = ImageView().fadeOnImageChange()
        private val imageViewLimitedPane = ImageViewLimitedPane(imageView, settings.imageDisplayTypeProperty)

        init {
            // Really annoying, no idea why JavaFX does this, but it offsets by 1 pixel.
            imageViewLimitedPane.translateX = -1.0

            imageViewLimitedPane.maxHeightProperty().bind(this.heightProperty())
            imageViewLimitedPane.maxWidthProperty().bind(this.widthProperty())
            imageViewLimitedPane.clip = createClippingArea()

            addClass(CommonStyle.card)
            val dropshadow = DropShadow().apply {
                input = Glow()
            }

            setOnMouseEntered { effect = dropshadow }
            setOnMouseExited { effect = null }
            graphic = imageViewLimitedPane
        }

        private fun createClippingArea(): Rectangle {
            val clip = Rectangle()
            clip.x = 1.0
            clip.y = 1.0
            clip.arcWidth = 20.0
            clip.arcHeight = 20.0
            clip.heightProperty().bind(imageViewLimitedPane.heightProperty().subtract(2))
            clip.widthProperty().bind(imageViewLimitedPane.widthProperty().subtract(2))
            return clip
        }

        override fun updateItem(game: Game?, empty: Boolean) {
            super.updateItem(game, empty)

            if (game != null) {
                val thumbnailUrl = game.thumbnailUrl
                val image = imageLoader.fetchImage(game.id, thumbnailUrl, persistIfAbsent = true)
                imageView.imageProperty().cleanBind(image)
                tooltip(game.name)
            } else {
                imageView.imageProperty().unbind()
                imageView.image = null
            }
        }
    }

    class Style : Stylesheet() {
        companion object {
            val quickDetails by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            quickDetails {
                padding = box(20.px)
                backgroundColor = multi(Color.LIGHTGRAY)

                label {
                    textFill = Color.BLACK
                }
            }
        }
    }
}
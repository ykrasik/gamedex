package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.preferences.GameWallPreferences
import com.gitlab.ykrasik.gamedex.ui.fadeOnImageChange
import com.gitlab.ykrasik.gamedex.ui.fragment.GameDetailsFragment
import com.gitlab.ykrasik.gamedex.ui.popOver
import com.gitlab.ykrasik.gamedex.ui.widgets.GameDetailSnippetFactory
import com.gitlab.ykrasik.gamedex.ui.widgets.ImageViewLimitedPane
import javafx.beans.property.ReadOnlyProperty
import javafx.css.StyleableObjectProperty
import javafx.scene.image.Image
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
class GameWallView : View("Games Wall") {
    private val controller: GameController by di()
    private val preferences: GameWallPreferences by di()
    private val imageLoader: ImageLoader by di()
    private val gameDetailSnippetFactory: GameDetailSnippetFactory by di()

    private val thumbnailCache = mutableMapOf<String?, ReadOnlyProperty<Image>>()

    override val root = datagrid(controller.games) {
        cellHeightProperty.bind(preferences.cellHeightProperty)
        cellWidthProperty.bind(preferences.cellWidthProperty)
        (horizontalCellSpacingProperty as StyleableObjectProperty).bind(preferences.cellHorizontalSpacingProperty)
        (verticalCellSpacingProperty as StyleableObjectProperty).bind(preferences.cellVerticalSpacingProperty)

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
                            contentNode = gameDetailSnippetFactory.create(cell.item, withDescription = false, withUrls = false).apply {
                                addClass(Style.quickDetails)
                            }
                            show(cell)
                        }
                    }
                    2 -> {
                        popOver.hide()
                        if (e.button == MouseButton.PRIMARY) {
                            GameDetailsFragment(cell.item).show()
                        }
                    }
                }
            }
            cell.contextmenu {
                menuitem("View Details") { GameDetailsFragment(cell.item).show() }
                menuitem("Change Thumbnail") { controller.changeThumbnail(cell.item) }
                separator()
                menuitem("Delete") { controller.delete(cell.item) }
            }
            cell
        }
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
        private val imageViewLimitedPane = ImageViewLimitedPane(imageView, preferences.imageDisplayTypeProperty)

        init {
            // Really annoying, no idea why JavaFX does this, but it offsets by 1 pixel.
            imageViewLimitedPane.translateX = -1.0

            imageViewLimitedPane.maxHeightProperty().bind(this.heightProperty())
            imageViewLimitedPane.maxWidthProperty().bind(this.widthProperty())
            imageViewLimitedPane.clip = createClippingArea()

            addClass(Styles.gameTile, Styles.card)
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
                val image = thumbnailCache.getOrPut(thumbnailUrl) { imageLoader.fetchImage(game.id, thumbnailUrl, persistIfAbsent = true) }
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
            }
        }
    }
}
package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.preferences.UserPreferences
import com.gitlab.ykrasik.gamedex.ui.fadeOnImageChange
import com.gitlab.ykrasik.gamedex.ui.fragment.GameDetailsFragment
import com.gitlab.ykrasik.gamedex.ui.widgets.ImageViewLimitedPane
import javafx.beans.property.ReadOnlyProperty
import javafx.css.StyleableObjectProperty
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.shape.Rectangle
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 15:03
 */
class GameWallView : View("Games Wall") {
    private val controller: GameController by di()
    private val userPreferences: UserPreferences by di()
    private val imageLoader: ImageLoader by di()

    private val thumbnailCache = mutableMapOf<String?, ReadOnlyProperty<Image>>()

    override val root = datagrid(controller.games) {
        cellHeightProperty.bind(userPreferences.gameWallCellHeightProperty)
        cellWidthProperty.bind(userPreferences.gameWallCellWidthProperty)
        (horizontalCellSpacingProperty as StyleableObjectProperty).bind(userPreferences.gameWallCellHorizontalSpacingProperty)
        (verticalCellSpacingProperty as StyleableObjectProperty).bind(userPreferences.gameWallCellVerticalSpacingProperty)

        cellFactory = {
            val cell = GameWallCell()
            cell.onDoubleClick {
                GameDetailsFragment(cell.item).show()
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

    // TODO: Allow to overlay the library name as a ribbon over the image.
    // TODO: Consider adding an option to display the game name under the cell
    inner class GameWallCell : DataGridCell<Game>(root) {
        private val imageView = ImageView().fadeOnImageChange()
        private val imageViewLimitedPane = ImageViewLimitedPane(imageView, userPreferences.gameWallImageDisplayTypeProperty)

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
}
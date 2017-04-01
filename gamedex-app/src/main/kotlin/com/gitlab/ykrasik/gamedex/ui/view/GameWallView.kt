package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.common.datamodel.Game
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.ui.controller.GameController
import com.gitlab.ykrasik.gamedex.ui.fadeOnImageChange
import com.gitlab.ykrasik.gamedex.ui.model.GameRepository
import com.gitlab.ykrasik.gamedex.ui.view.fragment.GameDetailsFragment
import com.gitlab.ykrasik.gamedex.ui.view.widgets.ImageViewLimitedPane
import com.gitlab.ykrasik.gamedex.util.UserPreferences
import javafx.beans.property.ReadOnlyProperty
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
    private val repository: GameRepository by di()
    private val userPreferences: UserPreferences by di()
    private val imageLoader: ImageLoader by di()

    private val thumbnailCache = mutableMapOf<Int, ReadOnlyProperty<Image>>()

    override val root = datagrid(repository.gamesProperty) {
        cellHeight = 192.0
        cellWidth = 136.0
        horizontalCellSpacing = 3.0
        verticalCellSpacing = 3.0

        cellFactory = {
            val cell = GameWallCell(userPreferences)
            cell.setOnMouseClicked { e ->
                if (e.clickCount == 2) {
                    GameDetailsFragment(cell.item).show()
//                    val search = URLEncoder.encode("${cell.item!!.name} pc gameplay", "utf-8")
//                    "https://www.youtube.com/results?search_query=$search".browseToUrl()
                }
            }
            cell.contextmenu {
                menuitem("Delete") { controller.delete(cell.item) }
            }
            cell
        }
    }

    inner class GameWallCell(userPreferences: UserPreferences) : DataGridCell<Game>(root) {
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

        override fun updateItem(item: Game?, empty: Boolean) {
            super.updateItem(item, empty)

            if (item != null) {
                val thumbnailId = item.imageIds.thumbnailId!!
                val image = thumbnailCache.getOrPut(thumbnailId) { imageLoader.fetchImage(thumbnailId) }
                imageView.imageProperty().bind(image)
            } else {
                imageView.image = null
            }
        }
    }
}
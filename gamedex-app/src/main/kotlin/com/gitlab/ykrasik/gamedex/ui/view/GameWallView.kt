package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.common.datamodel.Game
import com.gitlab.ykrasik.gamedex.common.util.browseToUrl
import com.gitlab.ykrasik.gamedex.common.util.mapped
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.ui.controller.GameController
import com.gitlab.ykrasik.gamedex.ui.fadeOnImageChange
import com.gitlab.ykrasik.gamedex.ui.model.GameRepository
import com.gitlab.ykrasik.gamedex.ui.view.widgets.ImageViewLimitedPane
import com.gitlab.ykrasik.gamedex.util.UserPreferences
import javafx.scene.image.ImageView
import javafx.scene.shape.Rectangle
import tornadofx.*
import java.net.URLEncoder

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

    private val gameViewProperty = repository.gamesProperty.mapped { GameViewItem(it) }

    override val root = datagrid(gameViewProperty) {
        cellHeight = 192.0
        cellWidth = 136.0
        horizontalCellSpacing = 3.0
        verticalCellSpacing = 3.0

        cellFactory = {
            val cell = GameWallCell(userPreferences)
            cell.setOnMouseClicked { e ->
                if (e.clickCount == 2) {
                    val search = URLEncoder.encode("${cell.item!!.game.name} pc gameplay", "utf-8")
                    "https://www.youtube.com/results?search_query=$search".browseToUrl()
                }
            }
            cell.contextmenu {
                menuitem("Delete") { controller.delete(cell.item.game) }
            }
            cell
        }
    }

    inner class GameWallCell(userPreferences: UserPreferences) : DataGridCell<GameViewItem>(root) {
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

        override fun updateItem(item: GameViewItem?, empty: Boolean) {
            super.updateItem(item, empty)

            if (item != null) {
                imageView.imageProperty().bind(item.thumbnailProperty)
            } else {
                imageView.image = null
            }
        }
    }

    inner class GameViewItem(val game: Game) {
        val thumbnailProperty by lazy { imageLoader.fetchImage(game.imageIds.thumbnailId) }
    }
}
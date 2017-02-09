package com.gitlab.ykrasik.gamedex.ui.view

import com.github.ykrasik.gamedex.common.datamodel.Game
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.ui.controller.GameController
import com.gitlab.ykrasik.gamedex.ui.gridView
import com.gitlab.ykrasik.gamedex.ui.model.GameRepository
import com.gitlab.ykrasik.gamedex.ui.view.widgets.ImageViewLimitedPane
import com.gitlab.ykrasik.gamedex.util.UserPreferences
import javafx.concurrent.Task
import javafx.scene.image.ImageView
import javafx.scene.shape.Rectangle
import org.controlsfx.control.GridCell
import tornadofx.View
import tornadofx.addClass
import tornadofx.contextmenu
import tornadofx.menuitem
import java.awt.Desktop
import java.net.URL
import java.net.URLEncoder

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 15:03
 */
class GameWallView : View("Games Wall") {
    private val controller: GameController by di()
    private val repository: GameRepository by di()
    private val imageLoader: ImageLoader by di()
    private val userPreferences: UserPreferences by di()

    override val root = gridView<Game> {
        itemsProperty().bind(repository.gamesProperty)

        cellHeight = 192.0
        cellWidth = 136.0
        horizontalCellSpacing = 3.0
        verticalCellSpacing = 3.0

        setCellFactory {
            val cell = GameWallCell(imageLoader, userPreferences)
            cell.setOnMouseClicked { e ->
                if (e.clickCount == 2) {
                    val search = URLEncoder.encode("${cell.item!!.name} pc gameplay")
                    val url = URL("https://www.youtube.com/results?search_query=$search")
                    Desktop.getDesktop().browse(url.toURI())
                }
            }
            cell.contextmenu {
                menuitem("Delete") { controller.delete(cell.item) }
            }
            cell
        }
    }
}

class GameWallCell(private val imageLoader: ImageLoader, userPreferences: UserPreferences) : GridCell<Game>() {
    private val imageView = ImageView()
    private val imageViewLimitedPane = ImageViewLimitedPane(imageView, userPreferences.gameWallImageDisplayTypeProperty)

    private var loadingTask: Task<*>? = null

    init {

        // Really annoying, no idea why JavaFX does this, but it offsets by 1 pixel.
        imageViewLimitedPane.translateX = -1.0

        imageViewLimitedPane.maxHeightProperty().bind(this.heightProperty())
        imageViewLimitedPane.maxWidthProperty().bind(this.widthProperty())
        imageViewLimitedPane.clip = createClippingArea()

//        val binding = MoreBindings.transformBinding(configService.gameWallImageDisplayProperty(), { imageDisplay ->
//            val image = imageView.image
//            if (image === UIResources.loading() || image === UIResources.notAvailable()) {
//                return@MoreBindings.transformBinding ImageDisplayType . FIT
//            } else {
//                return@MoreBindings.transformBinding imageDisplay . getImageDisplayType ()
//            }
//        })
//        imageView.imageProperty().addListener { observable, oldValue, newValue -> binding.invalidate() }
//        imageViewLimitedPane.imageDisplayTypeProperty().bind(binding)

        addClass(Styles.gameTile, Styles.card)
        addClass("image-grid-cell") //$NON-NLS-1$
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

        // FIXME: This could create race condition when an un-cancelled task finishes after the new task and overrides the cell's image.
//        cancelPrevTask()
        if (item != null) {
            fetchThumbnail(item)
        } else {
            imageView.image = null
        }
    }

    private fun fetchThumbnail(game: Game) {
        game.imageIds.thumbnailId?.let { thumbnailId ->
            loadingTask = imageLoader.loadImage(thumbnailId, imageView)
        }
    }

    private fun cancelPrevTask() {
        loadingTask?.cancel(false)
        loadingTask = null
    }
}
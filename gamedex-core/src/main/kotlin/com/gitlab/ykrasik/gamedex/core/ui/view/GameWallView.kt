package com.gitlab.ykrasik.gamedex.core.ui.view

import com.github.ykrasik.gamedex.datamodel.Game
import com.gitlab.ykrasik.gamedex.core.ui.controller.GameController
import com.gitlab.ykrasik.gamedex.core.ui.gridView
import com.gitlab.ykrasik.gamedex.core.ui.model.GamesModel
import com.gitlab.ykrasik.gamedex.core.ui.view.widgets.ImageViewLimitedPane
import javafx.event.EventHandler
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.util.Callback
import org.controlsfx.control.GridCell
import tornadofx.View
import tornadofx.addClass
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
    private val model: GamesModel by di()

    override val root = gridView<Game> {
        cellHeight = 192.0
        cellWidth = 136.0
        horizontalCellSpacing = 3.0
        verticalCellSpacing = 3.0

        cellFactory = Callback { gridView ->
            val cell = GameWallCell()
            cell.onMouseClicked = EventHandler<MouseEvent> { e ->
                if (e.clickCount == 2) {
                    val search = URLEncoder.encode("${cell.item!!.name} pc gameplay")
                    val url = URL("https://www.youtube.com/results?search_query=$search")
                    Desktop.getDesktop().browse(url.toURI())
                }
            }
            cell
        }

        itemsProperty().bind(model.allProperty)
    }
}

class GameWallCell : GridCell<Game>() {
    private val imageView = ImageView()
    private val imageViewLimitedPane = ImageViewLimitedPane(imageView)

//    private var loadingTask = Opt.none()

    init {

//        // Really annoying, no idea why JavaFX does this, but it offsets by 1 pixel.
//        imageViewLimitedPane.translateX = -1.0
//
//        imageViewLimitedPane.maxHeightProperty().bind(this.heightProperty())
//        imageViewLimitedPane.maxWidthProperty().bind(this.widthProperty())
//
//        // Clip the cell's corners to be round after the cell's size is calculated.
//        val clip = Rectangle()
//        clip.x = 1.0
//        clip.y = 1.0
//        clip.arcWidth = 20.0
//        clip.arcHeight = 20.0
//        val clipListener = ChangeListener<Number> { observable, oldValue, newValue ->
//            clip.width = imageViewLimitedPane.width - 2
//            clip.height = imageViewLimitedPane.height - 2
//        }
//        imageViewLimitedPane.clip = clip
//        imageViewLimitedPane.heightProperty().addListener(clipListener)
//        imageViewLimitedPane.widthProperty().addListener(clipListener)

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
        text = ""
    }

    override fun updateItem(item: Game?, empty: Boolean) {
//        cancelPrevTask()
        super.updateItem(item, empty)

        if (!empty) {
//            fetchImage(item)
//            graphic = imageViewLimitedPane
            text = item?.name
        } else {
//            graphic = null
            text = ""
        }
    }

//    private fun fetchImage(game: Game) {
//        loadingTask = Opt.some(imageService.fetchThumbnail(game.id, imageView))
//    }
//
//    private fun cancelPrevTask() {
//        if (loadingTask.isDefined()) {
//            val task = loadingTask.get()
//            if (task.getState() != Worker.State.SUCCEEDED && task.getState() != Worker.State.FAILED) {
//                task.cancel()
//            }
//        }
//        loadingTask = Opt.none()
//    }
}
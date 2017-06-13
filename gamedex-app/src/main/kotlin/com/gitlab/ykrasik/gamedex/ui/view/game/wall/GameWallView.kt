package com.gitlab.ykrasik.gamedex.ui.view.game.wall

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.settings.GameWallSettings
import com.gitlab.ykrasik.gamedex.ui.fadeOnImageChange
import com.gitlab.ykrasik.gamedex.ui.popOver
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.view.game.details.GameDetailsFragment
import com.gitlab.ykrasik.gamedex.ui.view.game.menu.GameContextMenu
import com.gitlab.ykrasik.gamedex.ui.widgets.ImageViewLimitedPane
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Glow
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Screen
import org.controlsfx.control.GridCell
import org.controlsfx.control.GridView
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 15:03
 */
class GameWallView : View("Games Wall") {
    private val gameController: GameController by di()
    private val imageLoader: ImageLoader by di()
    private val settings: GameWallSettings by di()

    private val gameContextMenu: GameContextMenu by inject()

    override val root = GridView(gameController.sortedFilteredGames).apply {
        cellHeightProperty().bind(settings.cellHeightProperty)
        cellWidthProperty().bind(settings.cellWidthProperty)
        horizontalCellSpacingProperty().bind(settings.cellHorizontalSpacingProperty)
        verticalCellSpacingProperty().bind(settings.cellVerticalSpacingProperty)

        val popOver = popOver().apply {
            addEventHandler(MouseEvent.MOUSE_PRESSED) { hide() }
        }

        setCellFactory {
            val cell = GameWallCell()
            cell.setOnMouseClicked { e ->
                when (e.clickCount) {
                    1 -> with(popOver) {
                        if (isShowing) {
                            hide()
                        } else if (e.button == MouseButton.PRIMARY) {
                            arrowLocation = determineArrowLocation(e.screenX, e.screenY)
                            contentNode = GameDetailsFragment(cell.item!!, withDescription = false, withUrls = false).root.apply {
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
            gameContextMenu.install(cell) { cell.item!! }
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

    // TODO: Consider adding an option to display the game name under the cell
    private inner class GameWallCell : GridCell<Game>() {
        private val imageView = ImageView().fadeOnImageChange()
        private val imageViewLimitedPane = ImageViewLimitedPane(imageView, settings.imageDisplayTypeProperty)

        private val overlay = Label().apply {
            addClass(Style.overlayText, CommonStyle.fillAvailableWidth)
            StackPane.setAlignment(this, Pos.BOTTOM_CENTER)
        }
        private val content = StackPane(imageViewLimitedPane, overlay)

        init {
            // Really annoying, no idea why JavaFX does this, but it offsets by 1 pixel.
            imageViewLimitedPane.translateX = -1.0

            imageViewLimitedPane.maxHeightProperty().bind(this.heightProperty())
            imageViewLimitedPane.maxWidthProperty().bind(this.widthProperty())

            addClass(CommonStyle.card)
            val dropshadow = DropShadow().apply { input = Glow() }
            setOnMouseEntered { effect = dropshadow }
            setOnMouseExited { effect = null }

            content.clip = createClippingArea()
            graphic = content
        }

        private fun createClippingArea() = Rectangle().apply {
            x = 1.0
            y = 1.0
            arcWidth = 20.0
            arcHeight = 20.0
            heightProperty().bind(imageViewLimitedPane.heightProperty().subtract(2))
            widthProperty().bind(imageViewLimitedPane.widthProperty().subtract(2))
        }

        override fun updateItem(game: Game?, empty: Boolean) {
            super.updateItem(game, empty)

            if (game != null) {
                game.folderMetaData.metaTag?.overlay() ?: clearOverlay()
                imageView.imageProperty().cleanBind(imageLoader.fetchImage(game.id, game.thumbnailUrl, persistIfAbsent = true))
                tooltip(game.name)
            } else {
                clearOverlay()
                imageView.imageProperty().unbind()
                imageView.image = null
            }
        }

        private fun String.overlay() {
            overlay.isVisible = true
            overlay.text = this
        }

        private fun clearOverlay() {
            overlay.isVisible = false
            overlay.text = null
        }
    }

    class Style : Stylesheet() {
        companion object {
            val quickDetails by cssclass()
            val overlayText by cssclass()

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

            overlayText {
                backgroundColor = multi(Color.LIGHTGRAY)
                opacity = 0.85
                padding = box(5.px)
                alignment = Pos.BASELINE_CENTER
            }
        }
    }
}
package com.gitlab.ykrasik.gamedex.ui.view.game.wall

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.settings.GameWallSettings
import com.gitlab.ykrasik.gamedex.ui.popOver
import com.gitlab.ykrasik.gamedex.ui.view.game.details.GameDetailsFragment
import com.gitlab.ykrasik.gamedex.ui.view.game.menu.GameContextMenu
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
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
        setId(Style.gameWall)

        cellHeightProperty().bind(settings.cell.heightProperty)
        cellWidthProperty().bind(settings.cell.widthProperty)
        horizontalCellSpacingProperty().bind(settings.cell.horizontalSpacingProperty)
        verticalCellSpacingProperty().bind(settings.cell.verticalSpacingProperty)

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
        private val fragment = GameWallCellFragment()
        private val imageView get() = fragment.imageView
        private val metaTagOverlay get() = fragment.metaTagOverlay
        private val versionOverlay get() = fragment.versionOverlay

        init {
            fragment.root.maxWidthProperty().bind(this.widthProperty())
            fragment.root.maxHeightProperty().bind(this.heightProperty())
            graphic = fragment.root

            settings.cell.imageDisplayTypeProperty.onChange { requestLayout() }
        }

        override fun updateItem(game: Game?, empty: Boolean) {
            super.updateItem(game, empty)

            if (game != null) {
                metaTagOverlay.text = game.folderMetadata.metaTag
                versionOverlay.text = game.folderMetadata.version
                imageView.imageProperty().cleanBind(imageLoader.fetchImage(game.id, game.thumbnailUrl, persistIfAbsent = true))
                tooltip(game.name)
            } else {
                metaTagOverlay.text = null
                versionOverlay.text = null
                imageView.imageProperty().unbind()
                imageView.image = null
            }
        }

        override fun resize(width: Double, height: Double) {
            imageView.isPreserveRatio = when (settings.cell.imageDisplayType) {
                GameWallSettings.ImageDisplayType.fit -> true
                GameWallSettings.ImageDisplayType.stretch -> isPreserveImageRatio()
                else -> throw IllegalArgumentException("Invalid imageDisplayType: ${settings.cell.imageDisplayType}")
            }

            super.resize(width, height)
        }

        private fun isPreserveImageRatio(): Boolean {
            if (imageView.image == null) return true

            val image = imageView.image

            // If the image is fit into the cell, this will be it's size.
            val heightRatio = height / image.height
            val widthRatio = width / image.width
            val fitRatio = Math.min(heightRatio, widthRatio)
            val imageFitHeight = image.height * fitRatio
            val imageFitWidth = image.width * fitRatio

            // Calculate the ratio by which we need to stretch the image to make it fill the whole cell.
            val stretchHeightRatio = height / imageFitHeight
            val stretchWidthRatio = width / imageFitWidth
            val stretchRatio = Math.max(stretchHeightRatio, stretchWidthRatio)

            // If stretchRatio isn't bigger than maxStretch, stretch the image.
            return Math.abs(stretchRatio - 1) > maxStretch
        }
    }

    class Style : Stylesheet() {
        companion object {
            val gameWall by cssid()
            val gameThumbnail by cssclass()
            val quickDetails by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            gameWall {
            }
            quickDetails {
                padding = box(20.px)
                backgroundColor = multi(Color.LIGHTGRAY)

                label {
                    textFill = Color.BLACK
                }
            }

            gameThumbnail {
            }
        }
    }

    companion object {
        // TODO: allow configuring this
        private val maxStretch = 0.35
    }
}
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
                metaTagOverlay.text = game.folderMetaData.metaTag
                versionOverlay.text = game.folderMetaData.version
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

//    private inner class GameWallCell : GridCell<Game>() {
//        private val imageView = ImageView().fadeOnImageChange()
//        private val imageViewLimitedPane = ImageViewLimitedPane(imageView, settings.cell.imageDisplayTypeProperty)
//
//        private val metaTagOverlay = Label().apply {
//            addClass(Style.overlayText, CommonStyle.fillAvailableWidth)
//            StackPane.setAlignment(this, Pos.BOTTOM_CENTER)
//        }
//        private val versionOverlay = Label().apply {
//            addClass(Style.overlayText, CommonStyle.fillAvailableWidth)
////            rotate = 45.0
//            StackPane.setAlignment(this, Pos.TOP_RIGHT)
////            minWidthProperty().bind(this@GameWallCell.widthProperty())
//        }
//        private val content = StackPane(imageView, metaTagOverlay)
////            minWidthProperty().bind(this@GameWallCell.widthProperty())
////            minHeightProperty().bind(this@GameWallCell.heightProperty())
////        })
//
//        init {
//            // Really annoying, no idea why JavaFX does this, but it offsets by 1 pixel.
////            imageViewLimitedPane.translateX = -1.0
//
//            imageView.fitWidthProperty().bind(this.widthProperty())
//            imageView.fitHeightProperty().bind(this.heightProperty())
//            imageView.isPreserveRatio = true
//
//            content.maxWidth = Double.MAX_VALUE
//            content.maxHeight = Double.MAX_VALUE
////            content.alignment = Pos.CENTER
//            content.minWidthProperty().bind(content.maxWidthProperty())
//            content.minHeightProperty().bind(content.maxHeightProperty())
//            content.maxWidthProperty().bind(imageView.boundsInParentProperty().map { it!!.width.toInt() })
//            content.maxHeightProperty().bind(imageView.boundsInParentProperty().map { it!!.height.toInt() })
//
//            addEventFilter(MouseEvent.MOUSE_CLICKED) {
//                println("imageView: " + imageView.boundsInParent)
//                println("content: width = ${content.width}, height = ${content.height}")
//                println("graphic: width = ${(graphic as StackPane).width}, height = ${(graphic as StackPane).height}")
//            }
//
////            imageViewLimitedPane.maxHeightProperty().bind(this.heightProperty())
////            imageViewLimitedPane.maxWidthProperty().bind(this.widthProperty())
//
////            addClass(CommonStyle.card)
//            val dropshadow = DropShadow().apply { input = Glow() }
//            setOnMouseEntered { effect = dropshadow }
//            setOnMouseExited { effect = null }
//
//            content.clip = createClippingArea()
//            graphic = StackPane(StackPane(content).apply {
//                addClass(Style.gameThumbnail)
//                maxWidthProperty().bind(content.maxWidthProperty())
//                maxHeightProperty().bind(content.maxHeightProperty())
//            }, Group(versionOverlay).apply {
//                StackPane.setAlignment(this, Pos.TOP_RIGHT)
//                addClass(CommonStyle.fillAvailableWidth)
//                prefWidthProperty().bind(content.maxWidthProperty())
//            }).apply {
//                //                minWidthProperty().bind(content.maxWidthProperty().add(30))
////                minHeightProperty().bind(content.maxHeightProperty().add(30))
//            }
//            alignment = Pos.CENTER
//        }
//
//        private fun createClippingArea() = Rectangle().apply {
//            //            x = 1.0
////            y = 1.0
//            arcWidth = 20.0
//            arcHeight = 20.0
//            heightProperty().bind(content.maxHeightProperty())
//            widthProperty().bind(content.maxWidthProperty())
//        }
//
//        override fun updateItem(game: Game?, empty: Boolean) {
//            super.updateItem(game, empty)
//
//            if (game != null) {
//                val metaTag = game.folderMetaData.metaTag
//                val version = game.folderMetaData.version
//                if (settings.metaTagOverlay.isShow && metaTag != null) {
//                    overlay(metaTagOverlay, metaTag)
//                } else {
//                    clearOverlay(metaTagOverlay)
//                }
//                if (settings.versionOverlay.isShow && version != null) {
//                    overlay(versionOverlay, version)
//                } else {
//                    clearOverlay(versionOverlay)
//                }
//                imageView.imageProperty().cleanBind(imageLoader.fetchImage(game.id, game.thumbnailUrl, persistIfAbsent = true))
//                tooltip(game.name)
//            } else {
//                clearOverlay(metaTagOverlay)
//                clearOverlay(versionOverlay)
//                imageView.imageProperty().unbind()
//                imageView.image = null
//            }
//        }
//
//        override fun resize(width: Double, height: Double) {
//            super.resize(width, height)
//        }
//
//        private fun overlay(label: Label, text: String) {
//            label.text = text
//            label.isVisible = true
//        }
//
//        private fun clearOverlay(label: Label) {
//            label.isVisible = false
//            label.text = null
//        }
//    }
//
//    class Style : Stylesheet() {
//        companion object {
//            val gameWall by cssid()
//            val gameThumbnail by cssclass()
//            val quickDetails by cssclass()
//            val overlayText by cssclass()
//
//            init {
//                importStylesheet(Style::class)
//            }
//        }
//
//        init {
//            gameWall {
//                //                backgroundColor = multi(RadialGradient())
//            }
//            quickDetails {
//                padding = box(20.px)
//                backgroundColor = multi(Color.LIGHTGRAY)
//
//                label {
//                    textFill = Color.BLACK
//                }
//            }
//
//            overlayText {
//                backgroundColor = multi(Color.LIGHTGRAY)
//                opacity = 0.85
//                padding = box(5.px)
//                alignment = Pos.BASELINE_CENTER
//            }
//
//            gameThumbnail {
//                borderColor = multi(box(Color.BLACK))
//                borderRadius = multi(box(10.px))
//                borderWidth = multi(box(1.px))
//            }
//        }
//    }
}
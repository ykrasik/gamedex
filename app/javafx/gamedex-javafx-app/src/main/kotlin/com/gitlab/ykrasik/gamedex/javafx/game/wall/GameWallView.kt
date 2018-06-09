/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.javafx.game.wall

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanShowGameDetails
import com.gitlab.ykrasik.gamedex.app.api.game.ViewWithGames
import com.gitlab.ykrasik.gamedex.app.api.settings.*
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.image.ImageLoader
import com.gitlab.ykrasik.gamedex.javafx.game.details.GameDetailsFragment
import com.gitlab.ykrasik.gamedex.javafx.game.menu.GameContextMenu
import com.gitlab.ykrasik.gamedex.javafx.map
import com.gitlab.ykrasik.gamedex.javafx.popOver
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableView
import com.gitlab.ykrasik.gamedex.javafx.settings.JavaFxCellDisplaySettings
import com.gitlab.ykrasik.gamedex.javafx.settings.JavaFxOverlayDisplaySettings
import com.gitlab.ykrasik.gamedex.javafx.sortedFiltered
import com.gitlab.ykrasik.gamedex.util.toPredicate
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.input.MouseButton
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
class GameWallView : PresentableView("Games Wall"), ViewWithGames, ViewCanShowGameDetails,
    ViewWithGameCellDisplaySettings, ViewWithNameOverlayDisplaySettings, ViewWithMetaTagOverlayDisplaySettings,
    ViewWithVersionOverlayDisplaySettings {

    override val games = mutableListOf<Game>().sortedFiltered()

    private val sortProperty = SimpleObjectProperty(Comparator.comparing(Game::name))
    override var sort by sortProperty

    private val filterProperty = SimpleObjectProperty { _: Game -> true }
    override var filter by filterProperty
    private val filterPredicateProperty = filterProperty.map { it!!.toPredicate() }

    override val showGameDetailsActions = channel<Game>()

    override val cellDisplaySettings = JavaFxCellDisplaySettings()
    override val nameOverlayDisplaySettings = JavaFxOverlayDisplaySettings()
    override val metaTagOverlayDisplaySettings = JavaFxOverlayDisplaySettings()
    override val versionOverlayDisplaySettings = JavaFxOverlayDisplaySettings()

    private val imageLoader: ImageLoader by di()

    private val gameContextMenu: GameContextMenu by inject()

    init {
        games.sortedItems.comparatorProperty().bind(sortProperty)
        games.filteredItems.predicateProperty().bind(filterPredicateProperty)
        viewRegistry.register(this)
    }

    override val root = GridView(games).apply {
        cellHeightProperty().bind(cellDisplaySettings.heightProperty)
        cellWidthProperty().bind(cellDisplaySettings.widthProperty)
        horizontalCellSpacingProperty().bind(cellDisplaySettings.horizontalSpacingProperty)
        verticalCellSpacingProperty().bind(cellDisplaySettings.verticalSpacingProperty)

        val popOver = popOver(closeOnClick = false)
        var popOverShowing = false

        setCellFactory {
            val cell = GameWallCell()
            cell.setOnMouseClicked { e ->
                popOver.setOnHidden {
                    cell.markSelected(false)
                }
                when (e.clickCount) {
                    1 -> with(popOver) {
                        if (popOverShowing) {
                            popOverShowing = false
                            hide()
                        } else if (e.button == MouseButton.PRIMARY) {
                            arrowLocation = determineArrowLocation(e.screenX, e.screenY)
                            contentNode = GameDetailsFragment(cell.item!!, withDescription = false).root.apply {
                                addClass(Style.quickDetails)
                            }
                            cell.markSelected(true)
                            show(cell)
                            popOverShowing = true
                        }
                    }
                    2 -> {
                        popOver.hide()
                        popOverShowing = false
                        if (e.button == MouseButton.PRIMARY) {
                            showGameDetailsActions.event(cell.item)
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

    private inner class GameWallCell : GridCell<Game>() {
        private val fragment = GameWallCellFragment(
            cellDisplaySettings, nameOverlayDisplaySettings, metaTagOverlayDisplaySettings, versionOverlayDisplaySettings
        )
        private val imageView get() = fragment.imageView
        private val nameOverlay get() = fragment.nameOverlay
        private val metaTagOverlay get() = fragment.metaTagOverlay
        private val versionOverlay get() = fragment.versionOverlay

        init {
            fragment.root.maxWidthProperty().bind(this.widthProperty())
            fragment.root.maxHeightProperty().bind(this.heightProperty())
            graphic = fragment.root

            cellDisplaySettings.imageDisplayTypeProperty.onChange { requestLayout() }
        }

        fun markSelected(selected: Boolean) {
            fragment.isSelected = selected
        }

        override fun updateItem(item: Game?, empty: Boolean) {
            super.updateItem(item, empty)

            if (item != null) {
                nameOverlay.text = item.name
                metaTagOverlay.text = item.folderMetadata.metaTag
                versionOverlay.text = item.folderMetadata.version
                imageView.imageProperty().cleanBind(imageLoader.fetchImage(item.thumbnailUrl, item.id, persistIfAbsent = true))
                tooltip(item.name)
            } else {
                metaTagOverlay.text = null
                versionOverlay.text = null
                imageView.imageProperty().unbind()
                imageView.image = null
            }
        }

        override fun resize(width: Double, height: Double) {
            imageView.isPreserveRatio = when (cellDisplaySettings.imageDisplayType) {
                ImageDisplayType.Fit, ImageDisplayType.FixedSize -> true
                ImageDisplayType.Stretch -> isPreserveImageRatio()
                else -> throw RuntimeException()
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

    companion object {
        // TODO: allow configuring this
        private val maxStretch = 0.35
    }
}
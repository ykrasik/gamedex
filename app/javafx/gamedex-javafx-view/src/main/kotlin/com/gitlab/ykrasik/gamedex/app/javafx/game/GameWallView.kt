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

package com.gitlab.ykrasik.gamedex.app.javafx.game

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanShowGameDetails
import com.gitlab.ykrasik.gamedex.app.api.game.ViewWithGames
import com.gitlab.ykrasik.gamedex.app.api.settings.*
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.game.details.JavaFxGameDetailsView
import com.gitlab.ykrasik.gamedex.app.javafx.image.ImageLoader
import com.gitlab.ykrasik.gamedex.app.javafx.settings.JavaFxGameWallDisplaySettings
import com.gitlab.ykrasik.gamedex.app.javafx.settings.JavaFxOverlayDisplaySettings
import com.gitlab.ykrasik.gamedex.javafx.control.determineArrowLocation
import com.gitlab.ykrasik.gamedex.javafx.control.popOver
import com.gitlab.ykrasik.gamedex.javafx.importStylesheetSafe
import com.gitlab.ykrasik.gamedex.javafx.sortedFiltered
import com.gitlab.ykrasik.gamedex.javafx.state
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.util.toPredicate
import javafx.scene.input.MouseButton
import javafx.scene.paint.Color
import javafx.util.Callback
import org.controlsfx.control.GridCell
import org.controlsfx.control.GridView
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 15:03
 */
class GameWallView : PresentableView("Games Wall"), ViewWithGames, ViewCanShowGameDetails,
    ViewWithGameWallDisplaySettings, ViewWithNameOverlayDisplaySettings, ViewWithMetaTagOverlayDisplaySettings,
    ViewWithVersionOverlayDisplaySettings {

    override val games = mutableListOf<Game>().sortedFiltered()

    override var sort = state(Comparator.comparing(Game::name))
    override val filter = state { _: Game -> true}

    override val showGameDetailsActions = channel<Game>()

    override val gameWallDisplaySettings = JavaFxGameWallDisplaySettings()
    override val nameOverlayDisplaySettings = JavaFxOverlayDisplaySettings()
    override val metaTagOverlayDisplaySettings = JavaFxOverlayDisplaySettings()
    override val versionOverlayDisplaySettings = JavaFxOverlayDisplaySettings()

    private val imageLoader: ImageLoader by di()

    private val gameContextMenu: GameContextMenu by inject()

    private val gameDetailsView = JavaFxGameDetailsView(withDescription = false)

    private val popOver = popOver(closeOnClick = false).apply {
        gameDetailsView.root.addClass(Style.quickDetails)
        contentNode = gameDetailsView.root
    }

    private var popOverShowing = false

    init {
        games.sortedItems.comparatorProperty().bind(sort.property)
        games.filteredItems.predicateProperty().bind(filter.property.objectBinding { it!!.toPredicate() })
        register()
    }

    override val root = GridView(games).apply {
        cellHeightProperty().bind(gameWallDisplaySettings.height.property)
        cellWidthProperty().bind(gameWallDisplaySettings.width.property)
        horizontalCellSpacingProperty().bind(gameWallDisplaySettings.horizontalSpacing.property)
        verticalCellSpacingProperty().bind(gameWallDisplaySettings.verticalSpacing.property)

        // Binding any observable properties inside the GameWallCell causes a memory leak -
        // the grid constantly constructs new instances of it, so if they retain a listener to the settings - we leak.
        // A workaround is to re-set the cellFactory whenever any settings change - this causes all cells to be rebuilt
        // without them having any listeners.
        gameWallDisplaySettings.onChange { setCellFactory(GameWallCellFactory()) }
        listOf(nameOverlayDisplaySettings, metaTagOverlayDisplaySettings, versionOverlayDisplaySettings).forEach {
            it.onChange { setCellFactory(GameWallCellFactory()) }
        }

        setCellFactory(GameWallCellFactory())
    }

    private inner class GameWallCellFactory : Callback<GridView<Game>, GridCell<Game>> {
        override fun call(param: GridView<Game>?): GridCell<Game> {
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
                            determineArrowLocation(e.screenX, e.screenY)
                            gameDetailsView.game.valueFromView = cell.item
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
            return cell
        }
    }

    private inner class GameWallCell : GridCell<Game>() {
        private val fragment = GameWallCellFragment(
            gameWallDisplaySettings, nameOverlayDisplaySettings, metaTagOverlayDisplaySettings, versionOverlayDisplaySettings
        )

        init {
            fragment.root.maxWidthProperty().bind(this.widthProperty())
            fragment.root.maxHeightProperty().bind(this.heightProperty())
            graphic = fragment.root
        }

        fun markSelected(selected: Boolean) {
            fragment.isSelected = selected
        }

        override fun updateItem(item: Game?, empty: Boolean) {
            super.updateItem(item, empty)

            if (item != null) {
                fragment.nameOverlay = item.name
                fragment.metaTagOverlay = item.folderNameMetadata.metaTag
                fragment.versionOverlay = item.folderNameMetadata.version
                fragment.setImage(imageLoader.fetchImage(item.thumbnailUrl, persistIfAbsent = true))
            } else {
                fragment.nameOverlay = null
                fragment.metaTagOverlay = null
                fragment.versionOverlay = null
                fragment.clearImage()
            }
        }

        override fun resize(width: Double, height: Double) {
            fragment.preserveRatio = when (gameWallDisplaySettings.imageDisplayType.value) {
                ImageDisplayType.Fit, ImageDisplayType.FixedSize -> true
                ImageDisplayType.Stretch -> isPreserveImageRatio()
                else -> kotlin.error("Invalid ImageDisplayType: ${gameWallDisplaySettings.imageDisplayType.value}")
            }

            super.resize(width, height)
        }

        private fun isPreserveImageRatio(): Boolean {
            val image = fragment.image ?: return true

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
                importStylesheetSafe(Style::class)
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
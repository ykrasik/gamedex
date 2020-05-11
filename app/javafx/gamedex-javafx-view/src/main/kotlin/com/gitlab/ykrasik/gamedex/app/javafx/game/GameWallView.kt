/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanOpenFile
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanShowGameDetails
import com.gitlab.ykrasik.gamedex.app.api.game.ViewGameParams
import com.gitlab.ykrasik.gamedex.app.api.settings.*
import com.gitlab.ykrasik.gamedex.app.api.util.broadcastFlow
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.app.javafx.settings.JavaFxGameWallDisplaySettings
import com.gitlab.ykrasik.gamedex.app.javafx.settings.JavaFxOverlayDisplaySettings
import com.gitlab.ykrasik.gamedex.javafx.JavaFxScope
import com.gitlab.ykrasik.gamedex.javafx.control.prettyGridView
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import javafx.collections.ObservableList
import javafx.scene.input.MouseButton
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.controlsfx.control.GridCell
import java.io.File

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 15:03
 */
class GameWallView(private val games: ObservableList<Game>) : PresentableView("Game Wall"),
    ViewCanShowGameDetails,
    ViewWithGameWallDisplaySettings,
    ViewWithNameOverlayDisplaySettings,
    ViewWithMetaTagOverlayDisplaySettings,
    ViewWithVersionOverlayDisplaySettings,
    ViewCanOpenFile {

    override val viewGameDetailsActions = broadcastFlow<ViewGameParams>()

    override val gameWallDisplaySettings = JavaFxGameWallDisplaySettings()
    override val nameOverlayDisplaySettings = JavaFxOverlayDisplaySettings()
    override val metaTagOverlayDisplaySettings = JavaFxOverlayDisplaySettings()
    override val versionOverlayDisplaySettings = JavaFxOverlayDisplaySettings()

    override val openFileActions = broadcastFlow<File>()

    private val commonOps: JavaFxCommonOps by di()

    private val gameContextMenu = GameContextMenu()

    override val root = prettyGridView(games) {
        cellHeightProperty().bind(gameWallDisplaySettings.height.property)
        cellWidthProperty().bind(gameWallDisplaySettings.width.property)
        horizontalCellSpacingProperty().bind(gameWallDisplaySettings.horizontalSpacing.property)
        verticalCellSpacingProperty().bind(gameWallDisplaySettings.verticalSpacing.property)

        // Binding any observable properties inside the GameWallCell causes a memory leak -
        // the grid constantly constructs new instances of it, so if they retain a listener to the settings - we leak.
        // A workaround is to re-set the cellFactory whenever any settings change - this causes all cells to be rebuilt
        // without them having any listeners.
        combine(gameWallDisplaySettings.changes(), nameOverlayDisplaySettings.changes(), metaTagOverlayDisplaySettings.changes(), versionOverlayDisplaySettings.changes()) { _ -> Unit }
            .onEach { setCellFactory { GameWallCell() } }
            .launchIn(JavaFxScope)
    }

    init {
        // This view must call init manually because it is not created via 'inject'
        init()

        register()
    }

    private inner class GameWallCell : GridCell<Game>() {
        private val fragment = GameWallCellFragment(
            gameWallDisplaySettings, nameOverlayDisplaySettings, metaTagOverlayDisplaySettings, versionOverlayDisplaySettings
        )

        init {
            setOnMousePressed { e ->
                if (e.button == MouseButton.PRIMARY) {
                    viewGameDetailsActions.event(ViewGameParams(item!!, games))
                }
            }
            gameContextMenu.install(this) { ViewGameParams(item!!, games) }

            fragment.root.maxWidthProperty().bind(this.widthProperty())
            fragment.root.maxHeightProperty().bind(this.heightProperty())
            graphic = fragment.root
        }

        override fun updateItem(item: Game?, empty: Boolean) {
            super.updateItem(item, empty)

            if (item != null) {
                fragment.nameOverlay = item.name
                fragment.metaTagOverlay = item.folderName.metaTag
                fragment.versionOverlay = item.folderName.version
                fragment.setImage(commonOps.fetchThumbnail(item))
            } else {
                fragment.nameOverlay = null
                fragment.metaTagOverlay = null
                fragment.versionOverlay = null
                fragment.clearImage()
            }
        }

        override fun resize(width: Double, height: Double) {
            fragment.preserveRatio = when (gameWallDisplaySettings.imageDisplayType.value) {
                ImageDisplayType.Fit, ImageDisplayType.Fixed -> true
                ImageDisplayType.Stretch -> isPreserveImageRatio()
                else -> error("Invalid ImageDisplayType: ${gameWallDisplaySettings.imageDisplayType.value}")
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

    companion object {
        // TODO: allow configuring this
        private val maxStretch = 0.35
    }
}
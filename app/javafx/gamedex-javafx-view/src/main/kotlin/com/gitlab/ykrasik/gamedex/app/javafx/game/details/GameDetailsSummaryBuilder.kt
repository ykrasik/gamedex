/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.javafx.game.details

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.javafx.control.defaultHbox
import com.gitlab.ykrasik.gamedex.javafx.control.defaultVbox
import com.gitlab.ykrasik.gamedex.javafx.control.imageview
import com.gitlab.ykrasik.gamedex.javafx.control.makeRoundCorners
import com.gitlab.ykrasik.gamedex.javafx.importStylesheetSafe
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.HPos
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.layout.GridPane
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 16/01/2019
 * Time: 08:38
 */
class GameDetailsSummaryBuilder : Fragment() {
    val commonOps: JavaFxCommonOps by di()

    var name: String? = null
    var nameOp: (Label.() -> Unit)? = null

    var platform: Platform? = null

    var description: String? = null
    var descriptionOp: (Label.() -> Unit)? = null

    var releaseDate: String? = null
    var releaseDateOp: (Label.() -> Unit)? = null

    var criticScore: Score? = null
    var criticScoreOp: (VBox.() -> Unit)? = null

    var userScore: Score? = null
    var userScoreOp: (VBox.() -> Unit)? = null

    var path: File? = null
    var pathOp: (Label.() -> Unit)? = null

    var image: ObservableValue<Image>? = null
    var imageFitWidth: Number = 200
    var imageOp: (VBox.() -> Unit)? = null

    var orientation: Orientation = Orientation.VERTICAL

    override val root = hbox()

    fun build(op: GridPane.() -> Unit = {}) = hbox(spacing = 15) {
        image?.let { image ->
            makeRoundCorners(imageview(image) {
                fitWidth = imageFitWidth.toDouble()
                fitHeightProperty().bind(root.heightProperty())
            }) {
                imageOp?.invoke(this)
            }
        }
        gridpane {
            hgap = 5.0
            vgap = 7.0
            alignment = Pos.TOP_RIGHT

            name?.let { name ->
                row {
                    platform?.logo.let {
                        if (it != null) icon(it, size = 22)
                        else children += Region()
                    }
                    header(name) {
                        isWrapText = true
                        addClass(Style.name)
                        nameOp?.invoke(this)
                    }
                }
            }
            releaseDate?.let { releaseDate ->
                row {
                    icon(Icons.date)
                    label(releaseDate) {
                        addClass(Style.releaseDate)
                        usePrefWidth = true
                        tooltip("Release Date")
                        releaseDateOp?.invoke(this)
                    }
                }
            }
            description?.let { description ->
                row {
                    region()
                    label(description) {
                        isWrapText = true
                        addClass(Style.descriptionText)
                        descriptionOp?.invoke(this)
                    }
                }
            }
            path?.let { path ->
                row {
                    icon(Icons.folder)
                    label(path.toString()) {
                        isWrapText = true
                        addClass(Style.path)
                        pathOp?.invoke(this)
                    }
                }
            }
            op(this)
        }

        spacer()

        if (orientation == Orientation.VERTICAL) {
            defaultVbox(alignment = Pos.TOP_RIGHT) {
                criticScoreDisplay(criticScore, criticScoreOp)
                userScoreDisplay(userScore, userScoreOp)
            }
        } else {
            defaultHbox(alignment = Pos.TOP_RIGHT) {
                criticScoreDisplay(criticScore, criticScoreOp)
                userScoreDisplay(userScore, userScoreOp)
            }
        }
    }

    private fun EventTarget.icon(icon: FontIcon, size: Int = 16) =
        add(icon.size(size).apply {
            gridpaneConstraints { vAlignment = VPos.TOP; hAlignment = HPos.RIGHT }
        })

    companion object {
        inline operator fun invoke(op: GameDetailsSummaryBuilder.() -> Unit = {}) = GameDetailsSummaryBuilder().apply(op)
        inline operator fun invoke(game: Game, op: GameDetailsSummaryBuilder.() -> Unit = {}) = invoke {
            name = game.name
            platform = game.platform
            releaseDate = game.releaseDate
            criticScore = game.criticScore
            userScore = game.userScore
            path = game.path
            imageFitWidth = 70
            image = commonOps.fetchThumbnail(game)
            orientation = Orientation.HORIZONTAL

            op()
        }
    }

    class Style : Stylesheet() {
        companion object {
            val name by cssclass()
            val path by cssclass()
            val descriptionText by cssclass()
            val releaseDate by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            name {
            }
            path {
            }
            descriptionText {
            }
        }
    }
}

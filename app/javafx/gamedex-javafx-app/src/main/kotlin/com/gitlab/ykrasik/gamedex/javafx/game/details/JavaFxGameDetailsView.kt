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

package com.gitlab.ykrasik.gamedex.javafx.game.details

import com.gitlab.ykrasik.gamedex.FileStructure
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanBrowseFile
import com.gitlab.ykrasik.gamedex.app.api.game.ViewWithGameFileStructure
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.image.ViewWithProviderLogos
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.api.web.ViewCanBrowseUrl
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.toHumanReadable
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventTarget
import javafx.geometry.HPos
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import org.joda.time.DateTimeZone
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:42
 */
class JavaFxGameDetailsView(
    private val withDescription: Boolean = true,
    private val evenIfEmpty: Boolean = false
) : PresentableView(), ViewWithProviderLogos, ViewWithGameFileStructure, ViewCanBrowseFile, ViewCanBrowseUrl {
    override val gameChanges = channel<Game>()
    private val gameProperty = SimpleObjectProperty<Game>().eventOnChange(gameChanges)
    override var game by gameProperty

    override var providerLogos = emptyMap<ProviderId, Image>()

    private val fileStructureProperty = SimpleObjectProperty<FileStructure>()
    override var fileStructure by fileStructureProperty

    private val fileStructurePlaceholder = label {
        minWidth = 60.0
        fileStructureProperty.onChange {
            text = it!!.size.humanReadable
        }
    }

    override val browseToFileActions = channel<File>()
    override val browseToUrlActions = channel<String>()

    init {
        viewRegistry.register(this)
    }

    override val root = stackpane {
        gameProperty.onChange {
            replaceChildren {
                gridpane {
                    hgap = 5.0
                    vgap = 8.0

                    name()
                    path()
                    if (withDescription) description()
                    releaseDate()
                    criticScore()
                    userScore()
                    genres()
                    tags()
                    urls()
                    timestamp()
                }
            }
        }
    }

    private fun GridPane.name() = row {
        children += game.platform.toLogo()
        label(game.name) {
            setId(Style.nameLabel)
            gridpaneConstraints { hAlignment = HPos.CENTER; hGrow = Priority.ALWAYS }
        }
        children += fileStructurePlaceholder
    }

    private fun GridPane.path() = row {
        detailsHeader("Path")
        jfxButton(game.path.toString()) {
            addClass(CommonStyle.hoverable, Style.detailsContent)
            isFocusTraversable = false
            eventOnAction(browseToFileActions) { game.path }
        }
    }

    private fun GridPane.description() = game.description.let { description ->
        if (description != null || evenIfEmpty) row {
            detailsHeader("Description")
            detailsContent(description.toDisplayString()) {
                isWrapText = true
                // TOOD: Allow expanding when it doesn't fit
                maxHeight = 400.0
            }
        }
    }

    private fun GridPane.releaseDate() = game.releaseDate.let { releaseDate ->
        if (releaseDate != null || evenIfEmpty) row {
            detailsHeader("Release Date")
            detailsContent(releaseDate.toDisplayString())
        }
    }

    private fun GridPane.criticScore() = score(game.criticScore, "Critic")
    private fun GridPane.userScore() = score(game.userScore, "User")
    private fun GridPane.score(score: Score?, name: String) {
        if (score != null || evenIfEmpty) row {
            detailsHeader("$name Score")
            hbox(spacing = 5.0) {
                if (score != null) {
                    fixedRating(10) { rating = score.score / 10 }
                    detailsContent(score.score.format(3))
                    detailsContent("Based on ${score.numReviews} $name reviews")
                } else {
                    noContent()
                }
            }
        }
    }

    private fun GridPane.genres() = elementList(game.genres, "Genres")
    private fun GridPane.tags() = elementList(game.tags, "Tags")
    private fun GridPane.elementList(elements: List<String>, name: String) {
        if (elements.isNotEmpty()) row {
            detailsHeader(name)
            hbox(spacing = 5.0) {
                if (elements.isNotEmpty()) {
                    elements.forEach { detailsContent(it) }
                } else {
                    noContent()
                }
            }
        }
    }

    private fun GridPane.urls() = game.rawGame.providerData.let { providerData ->
        if (providerData.isNotEmpty() || evenIfEmpty) row {
            detailsHeader("URL")
            if (providerData.isNotEmpty()) {
                gridpane {
                    hgap = 7.0
                    vgap = 3.0
                    providerData.sortedBy { it.header.id }.forEach { (header, gameData) ->
                        row {
                            children += providerLogos[header.id]!!.image.toImageView(height = 30.0, width = 70.0)
                            hyperlink(gameData.siteUrl) { eventOnAction(browseToUrlActions) { gameData.siteUrl } }
                        }
                    }
                }
            } else {
                noContent()
            }
        }
    }

    private fun GridPane.timestamp() {
        row {
            detailsHeader("Create Date")
            label(game.createDate.withZone(DateTimeZone.getDefault()).toHumanReadable())
        }
        row {
            detailsHeader("Update Date")
            label(game.updateDate.withZone(DateTimeZone.getDefault()).toHumanReadable())
        }
    }

    private fun EventTarget.detailsHeader(name: String) = label(name) { addClass(Style.detailsHeader) }
    private fun EventTarget.detailsContent(text: String, op: (Label.() -> Unit)? = null) = label(text) {
        addClass(Style.detailsContent)
        op?.invoke(this)
    }

    private fun EventTarget.noContent() = detailsContent(null.toDisplayString())

    private fun Double.format(digits: Int) = String.format("%.${digits}f", this)

    class Style : Stylesheet() {
        companion object {
            val detailsHeader by cssclass()
            val detailsContent by cssclass()
            val nameLabel by cssid()
            val genre by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            detailsHeader {
                minWidth = 80.px
            }
            detailsContent {
                borderRadius = multi(box(3.px))
                backgroundColor = multi(Color.LIGHTGRAY)
                backgroundRadius = multi(box(3.px))
                padding = box(vertical = 5.px, horizontal = 10.px)
            }
            nameLabel {
                fontSize = 20.px
            }
            genre {
                backgroundColor = multi(Color.LIGHTGRAY, Color.WHEAT)
            }
        }
    }
}
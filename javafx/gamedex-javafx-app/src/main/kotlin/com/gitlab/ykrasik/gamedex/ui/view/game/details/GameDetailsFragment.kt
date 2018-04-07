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

package com.gitlab.ykrasik.gamedex.ui.view.game.details

import com.github.thomasnield.rxkotlinfx.toBinding
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.core.api.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderRepository
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.provider.logoImage
import com.gitlab.ykrasik.gamedex.util.browseToUrl
import javafx.event.EventTarget
import javafx.geometry.HPos
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:42
 */
class GameDetailsFragment(
    private val game: Game,
    private val withDescription: Boolean = true,
    private val evenIfEmpty: Boolean = false
) : Fragment() {
    private val providerRepository: GameProviderRepository by di()
    private val fileSystemService: FileSystemService by di()

    override val root = gridpane {
        hgap = 5.0
        vgap = 8.0

        name()
        path()
        description()
        releaseDate()
        criticScore()
        userScore()
        genres()
        tags()
        urls()  // TODO: Should consider having a 'reviews' button.
    }

    private fun GridPane.name() = row {
        children += game.platform.toLogo()
        label(game.name) {
            setId(Style.nameLabel)
            gridpaneConstraints { hAlignment = HPos.CENTER; hGrow = Priority.ALWAYS }
        }
        label(fileSystemService.size(game.path).map { it.humanReadable }.toBinding()) { minWidth = 60.0 }
    }

    private fun GridPane.path() = row {
        detailsHeader("Path")
        pathButton(game.path) { addClass(CommonStyle.hoverable, Style.detailsContent) }
    }

    private fun GridPane.description() = game.description.let { description ->
        if (withDescription && (description != null || evenIfEmpty)) row {
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
                            children += providerRepository.provider(header.id).logoImage.toImageView(height = 30.0, width = 70.0)
                            hyperlink(gameData.siteUrl) { setOnAction { gameData.siteUrl.browseToUrl() } }
                        }
                    }
                }
            } else {
                noContent()
            }
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
                importStylesheet(Style::class)
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
package com.gitlab.ykrasik.gamedex.ui.view.game.details

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.fixedRating
import com.gitlab.ykrasik.gamedex.ui.imageview
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.pathButton
import com.gitlab.ykrasik.gamedex.ui.theme.toDisplayString
import com.gitlab.ykrasik.gamedex.ui.theme.toLogo
import com.gitlab.ykrasik.gamedex.util.browseToUrl
import javafx.event.EventTarget
import javafx.geometry.HPos
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:42
 */
class GameDetailsFragment(private val game: Game, withDescription: Boolean = true, withUrls: Boolean = true) : View() {
    private val providerRepository: GameProviderRepository by di()

    override val root = gridpane {
        hgap = 5.0
        vgap = 8.0

        name()
        path()
        if (withDescription) description()
        releaseDate()
        criticScore()
        userScore()
        if (game.genres.isNotEmpty()) genres()
        if (game.tags.isNotEmpty()) tags()
        if (withUrls) urls()
    }

    private fun GridPane.name() = row {
        children += game.platform.toLogo()
        label(game.name) {
            setId(Style.nameLabel)
            gridpaneConstraints { hAlignment = HPos.CENTER }
        }
    }

    private fun GridPane.path() = row {
        detailsHeader("Path")
        pathButton(game.path) { addClass(CommonStyle.hoverable, Style.detailsContent) }
    }

    private fun GridPane.description() = row {
        detailsHeader("Description")
        detailsContent(game.description.toDisplayString()) {
            isWrapText = true
            minHeight = Region.USE_PREF_SIZE
        }
    }

    private fun GridPane.releaseDate() = row {
        detailsHeader("Release Date")
        detailsContent(game.releaseDate.toDisplayString())
    }

    private fun GridPane.criticScore() = row {
        detailsHeader("Critic Score")
        score(game.criticScore, "critic")
    }

    private fun GridPane.userScore() = row {
        detailsHeader("User Score")
        score(game.userScore, "user")
    }

    private fun EventTarget.score(score: Score?, name: String) {
        hbox(spacing = 5.0) {
            if (score != null) {
                fixedRating(10) { rating = score.score / 10 }
                detailsContent(score.score.format(3))
                detailsContent("Based on ${score.numReviews} $name reviews")
            } else {
                detailsContent("NA")
            }
        }
    }

    private fun GridPane.genres() = row {
        detailsHeader("Genres")
        hbox(spacing = 5.0) {
            game.genres.forEach { detailsContent(it) }
        }
    }

    private fun GridPane.tags() = row {
        detailsHeader("Tags")
        hbox(spacing = 5.0) {
            game.tags.forEach { detailsContent(it) }
        }
    }

    private fun GridPane.urls() = row {
        detailsHeader("URL")
        gridpane {
            hgap = 7.0
            vgap = 3.0
            game.rawGame.providerData.sortedBy { it.header.id }.forEach { providerData ->
                row {
                    imageview(providerRepository.logo(providerData.header.id)) {
                        fitHeight = 30.0
                        fitWidth = 70.0
                        isPreserveRatio = true
                    }
                    hyperlink(providerData.gameData.siteUrl) {
                        setOnAction { providerData.gameData.siteUrl.browseToUrl() }
                    }
                }
            }
        }
    }

    private fun EventTarget.detailsHeader(name: String) = label(name) { addClass(Style.detailsHeader) }
    private fun EventTarget.detailsContent(text: String, op: (Label.() -> Unit)? = null) = label(text) {
        addClass(Style.detailsContent)
        op?.invoke(this)
    }

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
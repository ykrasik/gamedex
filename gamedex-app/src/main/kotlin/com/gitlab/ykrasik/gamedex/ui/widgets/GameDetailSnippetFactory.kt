package com.gitlab.ykrasik.gamedex.ui.widgets

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.fixedRating
import com.gitlab.ykrasik.gamedex.ui.imageview
import com.gitlab.ykrasik.gamedex.ui.jfxButton
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle.Companion.toDisplayString
import com.gitlab.ykrasik.gamedex.ui.theme.toLogo
import com.gitlab.ykrasik.gamedex.util.browseToUrl
import javafx.event.EventTarget
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import tornadofx.*
import java.awt.Desktop
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:42
 */
@Singleton
class GameDetailSnippetFactory @Inject constructor(private val providerRepository: GameProviderRepository) {

    fun create(game: Game,
               withDescription: Boolean = true,
               withUrls: Boolean = true,
               onGenrePressed: (String) -> Unit,
               onTagPressed: (String) -> Unit): VBox = VBox().apply {
        hbox {
            spacer()
            label(game.name) { setId(Style.nameLabel) }
            spacer()
            children += game.platform.toLogo()
        }
        gridpane {
            hgap = 5.0
            vgap = 8.0
            row {
                detailsLabel("Path")
                jfxButton(game.path.path) {
                    addClass(Style.details, CommonStyle.hoverable)
                    isFocusTraversable = false
                    setOnAction { Desktop.getDesktop().open(game.path) }
                }
            }
            if (withDescription) {
                row {
                    detailsLabel("Description")
                    label(game.description.toDisplayString()) {
                        addClass(Style.details)
                        isWrapText = true
                        minHeight = Region.USE_PREF_SIZE
                    }
                }
            }
            row {
                detailsLabel("Release Date")
                label(game.releaseDate.toDisplayString()) { addClass(Style.details) }
            }
            row {
                detailsLabel("Critic Score")
                gridpane {
                    hgap = 5.0
                    row {
                        score(game.criticScore, "critic")
                    }
                }
            }
            row {
                detailsLabel("User Score")
                gridpane {
                    hgap = 5.0
                    row {
                        score(game.userScore, "user")
                    }
                }
            }
            if (game.genres.isNotEmpty()) {
                row {
                    detailsLabel("Genres")
                    gridpane {
                        hgap = 5.0
                        row {
                            game.genres.forEach { genre ->
                                jfxButton(genre) {
                                    addClass(Style.details, CommonStyle.hoverable)
                                    setOnAction {
                                        onGenrePressed(genre)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (game.tags.isNotEmpty()) {
                row {
                    detailsLabel("Tags")
                    gridpane {
                        hgap = 5.0
                        row {
                            game.tags.forEach { tag ->
                                jfxButton(tag) {
                                    addClass(Style.details, CommonStyle.hoverable)
                                    setOnAction {
                                        onTagPressed(tag)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (withUrls) {
                row {
                    detailsLabel("URL")
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
            }
        }
    }

    private fun EventTarget.detailsLabel(name: String) = label(name) {
        addClass(Style.detailsLabel)
    }

    private fun EventTarget.score(score: Score?, name: String) {
        if (score == null) {
            label("NA") { addClass(Style.details)}
            return
        }

        fixedRating(10) { rating = score.score / 10 }
        label(score.score.format(3)) { addClass(Style.details) }
        label("Based on ${score.numReviews} $name reviews.") { addClass(Style.details) }
    }

    private fun Double.format(digits: Int) = String.format("%.${digits}f", this)

    class Style : Stylesheet() {
        companion object {
            val detailsLabel by cssclass()
            val details by cssclass()
            val nameLabel by cssid()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            detailsLabel {
                minWidth = 80.px
            }
            details {
                borderRadius = multi(box(3.px))
                backgroundColor = multi(Color.LIGHTGRAY)
                backgroundRadius = multi(box(3.px))
                padding = box(vertical = 5.px, horizontal = 10.px)
            }
            nameLabel {
                fontSize = 20.px
            }
        }
    }
}
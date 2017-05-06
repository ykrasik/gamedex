package com.gitlab.ykrasik.gamedex.ui.widgets

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.core.SortedFilteredGames
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.fixedRating
import com.gitlab.ykrasik.gamedex.ui.jfxButton
import com.gitlab.ykrasik.gamedex.ui.toLogo
import com.gitlab.ykrasik.gamedex.util.browseToUrl
import com.gitlab.ykrasik.gamedex.util.toStringOr
import javafx.event.EventTarget
import javafx.scene.effect.BlurType
import javafx.scene.effect.DropShadow
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import tornadofx.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:42
 */
@Singleton
class GameDetailSnippetFactory @Inject constructor(
    private val providerRepository: GameProviderRepository,
    private val sortedFilteredGames: SortedFilteredGames
) {

    fun create(game: Game,
               withDescription: Boolean = true,
               withUrls: Boolean = true,
               close: () -> Unit): VBox = VBox().apply {
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
                label(game.path.path) {
                    addClass(Style.details)
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
                        fixedRating(10) { rating = game.criticScore?.let { it / 10 } ?: 0.0 }
                        label(game.criticScore.toDisplayString()) {
                            addClass(Style.details)
                        }
                    }
                }
            }
            row {
                detailsLabel("User Score")
                gridpane {
                    hgap = 5.0
                    row {
                        fixedRating(10) { rating = game.userScore?.let { it / 10 } ?: 0.0 }
                        label(game.userScore.toDisplayString()) {
                            addClass(Style.details)
                        }
                    }
                }
            }
            row {
                detailsLabel("Genres")
                gridpane {
                    hgap = 5.0
                    row {
                        game.genres.forEach { genre ->
                            jfxButton(genre) {
                                addClass(Style.details, Style.genre)
                                setOnAction {
                                    sortedFilteredGames.genreFilterProperty.set(genre)
                                    close()
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
                        game.providerData.forEach { providerData ->
                            row {
                                imageview {
                                    fitHeight = 30.0
                                    fitWidth = 30.0
                                    image = providerRepository.logo(providerData)
                                }
                                hyperlink(providerData.siteUrl) {
                                    setOnAction { providerData.siteUrl.browseToUrl() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Any?.toDisplayString() = toStringOr("NA")

    private fun EventTarget.detailsLabel(name: String) = label(name) {
        addClass(Style.detailsLabel)
    }

    class Style : Stylesheet() {
        companion object {
            val detailsLabel by cssclass()
            val details by cssclass()
            val nameLabel by cssid()
            val genre by cssclass()

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
            genre {
                and(hover) {
                    translateX = 1.px
                    translateY = 1.px
                    effect = DropShadow(BlurType.GAUSSIAN, Color.web("#0093ff"), 12.0, 0.2, 0.0, 1.0)
                }
            }
        }
    }
}
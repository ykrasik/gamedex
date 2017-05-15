package com.gitlab.ykrasik.gamedex.ui.widgets

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.util.browseToUrl
import com.gitlab.ykrasik.gamedex.util.toStringOr
import javafx.beans.property.ObjectProperty
import javafx.event.EventTarget
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
class GameDetailSnippetFactory @Inject constructor(private val providerRepository: GameProviderRepository) {

    fun create(gameProperty: ObjectProperty<Game>,
               withDescription: Boolean = true,
               withUrls: Boolean = true,
               onGenrePressed: (String) -> Unit): VBox = VBox().apply {
        hbox {
            spacer()
            label(gameProperty.map { it!!.name }) { setId(Style.nameLabel) }
            spacer()
            stackpane {
                gameProperty.map { it!!.platform }.perform {
                    replaceChildren(it.toLogo())
                }
            }
        }
        gridpane {
            hgap = 5.0
            vgap = 8.0
            row {
                detailsLabel("Path")
                label(gameProperty.map { it!!.path.path }) {
                    addClass(Style.details)
                }
            }
            if (withDescription) {
                row {
                    detailsLabel("Description")
                    label(gameProperty.map { it!!.description.toDisplayString() }) {
                        addClass(Style.details)
                        isWrapText = true
                        minHeight = Region.USE_PREF_SIZE
                    }
                }
            }
            row {
                detailsLabel("Release Date")
                label(gameProperty.map { it!!.releaseDate.toDisplayString() }) { addClass(Style.details) }
            }
            row {
                detailsLabel("Critic Score")
                gridpane {
                    hgap = 5.0
                    row {
                        val criticScoreProperty = gameProperty.map { it!!.criticScore }
                        fixedRating(10) { ratingProperty().bind(criticScoreProperty.map { it?.let { it / 10 } ?: 0.0 }) }
                        label(criticScoreProperty.map { it.toDisplayString() }) {
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
                        val userScoreProperty = gameProperty.map { it!!.userScore }
                        fixedRating(10) { ratingProperty().bind(userScoreProperty.map { it?.let { it / 10 } ?: 0.0 }) }
                        label(userScoreProperty.map { it.toDisplayString() }) {
                            addClass(Style.details)
                        }
                    }
                }
            }
            row {
                detailsLabel("Genres")
                stackpane {
                    gameProperty.map { it!!.genres }.perform { genres ->
                        replaceChildren {
                            gridpane {
                                hgap = 5.0
                                row {
                                    genres.forEach { genre ->
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
                }
            }
            if (withUrls) {
                row {
                    detailsLabel("URL")
                    stackpane {
                        gameProperty.map { it!!.providerHeaders }.perform { providerHeaders ->
                            replaceChildren {
                                gridpane {
                                    hgap = 7.0
                                    providerHeaders.forEach { header ->
                                        row {
                                            imageview {
                                                fitHeight = 30.0
                                                fitWidth = 30.0
                                                image = providerRepository.logo(header)
                                            }
                                            hyperlink(header.siteUrl) {
                                                setOnAction { header.siteUrl.browseToUrl() }
                                            }
                                        }
                                    }
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
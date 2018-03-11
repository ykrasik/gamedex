package com.gitlab.ykrasik.gamedex.ui.view.settings

import com.gitlab.ykrasik.gamedex.ProviderId
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.repository.logoImage
import com.gitlab.ykrasik.gamedex.settings.ProviderSettings
import com.gitlab.ykrasik.gamedex.ui.perform
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.toImageView
import javafx.beans.property.ObjectProperty
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Glow
import javafx.scene.layout.Pane
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 15:17
 */
class ProviderOrderSettingsView : View("Order", Theme.Icon.settings()) {
    private val providerRepository: GameProviderRepository by di()
    private val settings: ProviderSettings by di()

    override val root = form {
        // FIXME: Forms take a long time to load!!!
        fieldset("Order Priorities") {
            listOf(
                "Search" to settings.searchOrderProperty,
                "Name" to settings.nameOrderProperty,
                "Description" to settings.descriptionOrderProperty,
                "Release Date" to settings.releaseDateOrderProperty,
                "Critic Score" to settings.criticScoreOrderProperty,
                "User Score" to settings.userScoreOrderProperty,
                "Thumbnail" to settings.thumbnailOrderProperty,
                "Poster" to settings.posterOrderProperty,
                "Screenshots" to settings.screenshotOrderProperty
            ).forEach { (name, orderProperty) ->
                field(name) {
                    providerOrder(orderProperty)
                }
            }
        }
    }

    private fun Pane.providerOrder(orderProperty: ObjectProperty<ProviderSettings.Order>) {
        hbox(spacing = 20.0) {
            alignment = Pos.CENTER
            orderProperty.perform { order ->
                var dragging: ProviderId? = null
                replaceChildren {
                    order.ordered().map { providerId ->
                        label {
                            addClass(Style.providerOrderLabel)
                            graphic = providerRepository.allProviders.find { it.id == providerId }!!.logoImage.toImageView(height = 50.0, width = 100.0)
                            userData = providerId

                            val dropShadow = DropShadow()
                            val glow = Glow()
                            effect = dropShadow

                            var dragX = 0.0

                            setOnMousePressed { mouseEvent ->
                                // record a delta distance for the drag and drop operation.
                                dragX = layoutX - mouseEvent.sceneX
                                cursor = Cursor.MOVE
                                dragging = providerId
                                this@hbox.children.forEach { it.isManaged = false }
                            }
                            setOnMouseReleased {
                                cursor = Cursor.HAND
                                dragging = null
                                this@hbox.children.forEach { it.isManaged = true }
                            }
                            setOnMouseDragged { mouseEvent ->
                                layoutX = mouseEvent.sceneX + dragX
                                val intersect = this@hbox.children.find { label ->
                                    this@label != label && this@label.boundsInParent.intersects(label.boundsInParent)
                                }
                                if (intersect != null) {
                                    orderProperty.value = order.switch(
                                        dragging!!,
                                        intersect.userData as ProviderId
                                    )
                                }
                            }
                            setOnMouseEntered {
                                cursor = Cursor.HAND
                                dropShadow.input = glow
                            }
                            setOnMouseExited {
                                dropShadow.input = null
                            }
                        }
                    }
                }
            }
        }
    }

    class Style : Stylesheet() {
        companion object {
            val providerOrderLabel by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            providerOrderLabel {
                prefWidth = 100.px
                alignment = Pos.BASELINE_CENTER
            }
        }
    }
}
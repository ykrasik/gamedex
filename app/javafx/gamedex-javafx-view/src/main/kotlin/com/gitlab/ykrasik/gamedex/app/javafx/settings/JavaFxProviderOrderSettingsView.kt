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

package com.gitlab.ykrasik.gamedex.app.javafx.settings

import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.settings.Order
import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderOrderSettingsView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.javafx.Theme
import com.gitlab.ykrasik.gamedex.javafx.importStylesheetSafe
import com.gitlab.ykrasik.gamedex.javafx.perform
import com.gitlab.ykrasik.gamedex.javafx.toImageView
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableTabView
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import javafx.beans.property.SimpleObjectProperty
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
class JavaFxProviderOrderSettingsView : PresentableTabView("Order", Theme.Icon.settings()), ProviderOrderSettingsView {
    override var providerLogos = emptyMap<ProviderId, Image>()

    override val searchChanges = channel<Order>()
    private val searchProperty = SimpleObjectProperty<Order>(emptyMap()).eventOnChange(searchChanges)
    override var search by searchProperty

    override val nameChanges = channel<Order>()
    private val nameProperty = SimpleObjectProperty<Order>(emptyMap()).eventOnChange(nameChanges)
    override var name by nameProperty

    override val descriptionChanges = channel<Order>()
    private val descriptionProperty = SimpleObjectProperty<Order>(emptyMap()).eventOnChange(descriptionChanges)
    override var description by descriptionProperty

    override val releaseDateChanges = channel<Order>()
    private val releaseDateProperty = SimpleObjectProperty<Order>(emptyMap()).eventOnChange(releaseDateChanges)
    override var releaseDate by releaseDateProperty

    override val criticScoreChanges = channel<Order>()
    private val criticScoreProperty = SimpleObjectProperty<Order>(emptyMap()).eventOnChange(criticScoreChanges)
    override var criticScore by criticScoreProperty

    override val userScoreChanges = channel<Order>()
    private val userScoreProperty = SimpleObjectProperty<Order>(emptyMap()).eventOnChange(userScoreChanges)
    override var userScore by userScoreProperty

    override val thumbnailChanges = channel<Order>()
    private val thumbnailProperty = SimpleObjectProperty<Order>(emptyMap()).eventOnChange(thumbnailChanges)
    override var thumbnail by thumbnailProperty

    override val posterChanges = channel<Order>()
    private val posterProperty = SimpleObjectProperty<Order>(emptyMap()).eventOnChange(posterChanges)
    override var poster by posterProperty

    override val screenshotChanges = channel<Order>()
    private val screenshotProperty = SimpleObjectProperty<Order>(emptyMap()).eventOnChange(screenshotChanges)
    override var screenshot by screenshotProperty

    init {
        viewRegistry.register(this)
    }

    override val root = form {
        // TODO: Forms take a long time to load!!!
        fieldset("Order Priorities") {
            listOf(
                "Search" to searchProperty,
                "Name" to nameProperty,
                "Description" to descriptionProperty,
                "Release Date" to releaseDateProperty,
                "Critic Score" to criticScoreProperty,
                "User Score" to userScoreProperty,
                "Thumbnail" to thumbnailProperty,
                "Poster" to posterProperty,
                "Screenshots" to screenshotProperty
            ).forEach { (name, orderProperty) ->
                field(name) {
                    providerOrder(orderProperty)
                }
            }
        }
    }

    private fun Pane.providerOrder(orderProperty: SimpleObjectProperty<Order>) {
        hbox(spacing = 20.0) {
            alignment = Pos.CENTER
            orderProperty.perform { order ->
                val ordered = order.entries.sortedBy { it.value }.map { it.key }
                var dragging: ProviderId? = null
                replaceChildren {
                    ordered.map { providerId ->
                        label {
                            addClass(Style.providerOrderLabel)
                            graphic = providerLogos[providerId]!!.image.toImageView(height = 50.0, width = 100.0)
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
                                    orderProperty.value = order.switch(dragging!!, intersect.userData as ProviderId)
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

    private fun Order.switch(a: ProviderId, b: ProviderId): Order {
        val currentA = get(a)!!
        val currentB = get(b)!!
        return this + (a to currentB) + (b to currentA)
    }

    class Style : Stylesheet() {
        companion object {
            val providerOrderLabel by cssclass()

            init {
                importStylesheetSafe(Style::class)
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
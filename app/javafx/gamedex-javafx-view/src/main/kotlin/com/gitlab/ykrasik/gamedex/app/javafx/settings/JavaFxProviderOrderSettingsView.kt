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
import com.gitlab.ykrasik.gamedex.app.api.image.ViewWithProviderLogos
import com.gitlab.ykrasik.gamedex.app.api.settings.Order
import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderOrderSettingsView
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.horizontalField
import com.gitlab.ykrasik.gamedex.javafx.control.toImageView
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableTabView
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Glow
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 15:17
 */
class JavaFxProviderOrderSettingsView : PresentableTabView("Order", Icons.sortAlphabetical), ProviderOrderSettingsView, ViewWithProviderLogos {
    override var providerLogos = emptyMap<ProviderId, Image>()

    override val search = userMutableState<Order>(emptyMap())
    override val name = userMutableState<Order>(emptyMap())
    override val description = userMutableState<Order>(emptyMap())
    override val releaseDate = userMutableState<Order>(emptyMap())
    override val criticScore = userMutableState<Order>(emptyMap())
    override val userScore = userMutableState<Order>(emptyMap())
    override val thumbnail = userMutableState<Order>(emptyMap())
    override val poster = userMutableState<Order>(emptyMap())
    override val screenshot = userMutableState<Order>(emptyMap())

    init {
        register()
    }

    override val root = form {
        fieldset("Order Priorities") {
            listOf(
                Triple("Search", Icons.search, search),
                Triple("Name", Icons.text, name),
                Triple("Description", Icons.textbox, description),
                Triple("Release Date", Icons.date, releaseDate),
                Triple("Critic Score", Icons.starFull, criticScore),
                Triple("User Score", Icons.starEmpty, userScore),
                Triple("Thumbnail", Icons.thumbnail, thumbnail),
                Triple("Poster", Icons.poster, poster),
                Triple("Screenshots", Icons.screenshots, screenshot)
            ).forEach { (name, icon, order) ->
                horizontalField(name) {
                    label.graphic = icon.color(Color.BLACK)
                    providerOrder(order.property)
                }
            }
        }
    }

    private fun Pane.providerOrder(orderProperty: SimpleObjectProperty<Order>) {
        hbox(spacing = 20.0, alignment = Pos.CENTER) {
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
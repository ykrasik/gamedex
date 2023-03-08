/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.app.api.settings.Order
import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderOrderSettingsView
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.javafx.control.enableWhen
import com.gitlab.ykrasik.gamedex.javafx.control.errorTooltip
import com.gitlab.ykrasik.gamedex.javafx.control.horizontalField
import com.gitlab.ykrasik.gamedex.javafx.control.toImageView
import com.gitlab.ykrasik.gamedex.javafx.importStylesheetSafe
import com.gitlab.ykrasik.gamedex.javafx.mutableStateFlow
import com.gitlab.ykrasik.gamedex.javafx.perform
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.color
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableTabView
import com.gitlab.ykrasik.gamedex.javafx.viewMutableStateFlow
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.IsValid
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
class JavaFxProviderOrderSettingsView : PresentableTabView("Order", Icons.sortAlphabetical), ProviderOrderSettingsView {
    private val commonOps: JavaFxCommonOps by di()

    override val canChangeProviderOrder = mutableStateFlow(IsValid.valid, debugName = "canChangeProviderOrder")

    override val search = viewMutableStateFlow<Order>(emptyList(), debugName = "search")
    override val name = viewMutableStateFlow<Order>(emptyList(), debugName = "name")
    override val description = viewMutableStateFlow<Order>(emptyList(), debugName = "description")
    override val releaseDate = viewMutableStateFlow<Order>(emptyList(), debugName = "releaseDate")
    override val thumbnail = viewMutableStateFlow<Order>(emptyList(), debugName = "thumbnail")
    override val poster = viewMutableStateFlow<Order>(emptyList(), debugName = "poster")
    override val screenshot = viewMutableStateFlow<Order>(emptyList(), debugName = "screenshot")

    init {
        register()
    }

    override val root = vbox {
        // Need to wrap in a vbox because the root's disabledProperty cannot be bound,
        // it is set by the tabPane which contains this view.
        errorTooltip(canChangeProviderOrder)
        form {
            enableWhen(canChangeProviderOrder, wrapInErrorTooltip = false)
            fieldset("Order Priorities") {
                listOf(
                    Triple("Search", Icons.search, search),
                    Triple("Name", Icons.text, name),
                    Triple("Description", Icons.textbox, description),
                    Triple("Release Date", Icons.date, releaseDate),
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
    }

    private fun Pane.providerOrder(orderProperty: SimpleObjectProperty<Order>) {
        hbox(spacing = 20, alignment = Pos.CENTER) {
            orderProperty.perform { order ->
                var dragging: ProviderId? = null
                replaceChildren {
                    order.map { providerId ->
                        label {
                            addClass(Style.providerOrderLabel)
                            graphic = commonOps.providerLogo(providerId).toImageView(height = 50.0, width = 100.0)
                            userData = providerId

                            val dropShadow = DropShadow()
                            val glow = Glow()
                            effect = dropShadow

                            var dragX = 0.0

                            setOnMousePressed { e ->
                                // record a delta distance for the drag and drop operation.
                                dragX = layoutX - e.sceneX
                                cursor = Cursor.MOVE
                                dragging = providerId
                                this@hbox.children.forEach { it.isManaged = false }
                                e.consume()
                            }
                            setOnMouseReleased {
                                cursor = Cursor.HAND
                                dragging = null
                                this@hbox.children.forEach { it.isManaged = true }
                            }
                            setOnMouseDragged { e ->
                                layoutX = e.sceneX + dragX
                                val intersect = this@hbox.children.find { label ->
                                    this@label != label && this@label.boundsInParent.intersects(label.boundsInParent)
                                }
                                if (intersect != null) {
                                    orderProperty.value = order.swap(dragging!!, intersect.userData as ProviderId)
                                }
                                e.consume()
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

    private fun Order.swap(a: ProviderId, b: ProviderId): Order = map {
        when (it) {
            a -> b
            b -> a
            else -> it
        }
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
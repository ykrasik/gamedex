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

package com.gitlab.ykrasik.gamedex.ui.view.settings

import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderRepository
import com.gitlab.ykrasik.gamedex.core.api.util.value_
import com.gitlab.ykrasik.gamedex.core.provider.ProviderUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import com.gitlab.ykrasik.gamedex.javafx.Theme
import com.gitlab.ykrasik.gamedex.javafx.provider.logoImage
import com.gitlab.ykrasik.gamedex.javafx.toImageView
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import io.reactivex.subjects.BehaviorSubject
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
    private val userConfigRepository: UserConfigRepository by di()
    private val providerUserConfig = userConfigRepository[ProviderUserConfig::class]

    override val root = form {
        // FIXME: Forms take a long time to load!!!
        fieldset("Order Priorities") {
            listOf(
                "Search" to providerUserConfig.searchOrderSubject,
                "Name" to providerUserConfig.nameOrderSubject,
                "Description" to providerUserConfig.descriptionOrderSubject,
                "Release Date" to providerUserConfig.releaseDateOrderSubject,
                "Critic Score" to providerUserConfig.criticScoreOrderSubject,
                "User Score" to providerUserConfig.userScoreOrderSubject,
                "Thumbnail" to providerUserConfig.thumbnailOrderSubject,
                "Poster" to providerUserConfig.posterOrderSubject,
                "Screenshots" to providerUserConfig.screenshotOrderSubject
            ).forEach { (name, orderSubject) ->
                field(name) {
                    providerOrder(orderSubject)
                }
            }
        }
    }

    private fun Pane.providerOrder(orderSubject: BehaviorSubject<ProviderUserConfig.Order>) {
        hbox(spacing = 20.0) {
            alignment = Pos.CENTER
            orderSubject.subscribe { order ->
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
                                    orderSubject.value_ = order.switch(dragging!!, intersect.userData as ProviderId)
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
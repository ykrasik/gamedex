/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.javafx.image

import com.gitlab.ykrasik.gamedex.app.api.image.ImageGalleryView
import com.gitlab.ykrasik.gamedex.app.api.image.ViewImageParams
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.Colors
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.size
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.geometry.Pos
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 12/05/2019
 * Time: 09:05
 */
class JavaFxImageGalleryView : PresentableView(), ImageGalleryView {
    private val commonOps: JavaFxCommonOps by di()

    override val imageParams = viewMutableStatefulChannel(ViewImageParams(imageUrl = "", imageUrls = emptyList()))
    override val currentImageIndex = statefulChannel(-1)

    override val canViewNextImage = statefulChannel(IsValid.valid)
    override val viewNextImageActions = channel<Unit>()

    override val canViewPrevImage = statefulChannel(IsValid.valid)
    override val viewPrevImageActions = channel<Unit>()

    private var slideDirection = ViewTransition.Direction.LEFT

    init {
        register()
    }

    val customizeOverlay: OverlayPane.OverlayLayer.() -> Unit = {
        val overlayPane = this

        stackpane {
            addClass(Style.arrowLeftContainer)
            maxWidth = Region.USE_PREF_SIZE
            maxHeight = Region.USE_PREF_SIZE
            stackpaneConstraints { alignment = Pos.CENTER_LEFT }
            visibleProperty().bind(overlayPane.hidingProperty.not() and overlayPane.activeProperty)
            jfxButton(graphic = Icons.arrowLeftBold.size(120)) {
                addClass(Style.arrowLeft)
                visibleWhen(canViewPrevImage)
                scaleOnMouseOver(duration = 0.2.seconds, target = 1.1)
                overlayPane.addEventFilter(KeyEvent.KEY_PRESSED) { e ->
                    if (e.code == KeyCode.LEFT && canViewPrevImage.value.isSuccess) {
                        flashColor(duration = 0.2.seconds, from = Colors.transparentWhite, to = Color.WHITE)
                        flashScale(duration = 0.2.seconds, target = 1.1)
                        fire()
                    }
                }
                action(viewPrevImageActions) { slideDirection = ViewTransition.Direction.RIGHT }
            }
        }

        stackpane {
            addClass(Style.arrowRightContainer)
            maxWidth = Region.USE_PREF_SIZE
            maxHeight = Region.USE_PREF_SIZE
            stackpaneConstraints { alignment = Pos.CENTER_RIGHT }
            visibleProperty().bind(overlayPane.hidingProperty.not() and overlayPane.activeProperty)
            jfxButton(graphic = Icons.arrowRightBold.size(120)) {
                addClass(Style.arrowRight)
                visibleWhen(canViewNextImage)
                scaleOnMouseOver(duration = 0.2.seconds, target = 1.1)
                overlayPane.addEventFilter(KeyEvent.KEY_PRESSED) { e ->
                    if (e.code == KeyCode.RIGHT && canViewNextImage.value.isSuccess) {
                        flashColor(duration = 0.2.seconds, from = Colors.transparentWhite, to = Color.WHITE)
                        flashScale(duration = 0.2.seconds, target = 1.1)
                        fire()
                    }
                }
                action(viewNextImageActions) { slideDirection = ViewTransition.Direction.LEFT }
            }
        }
    }

    override val root = stackpane {
        children += imageView(imageParams.value)

        imageParams.property.typeSafeOnChange { params ->
            val new = imageView(params)
            children[0].replaceWith(new, ViewTransition.Slide(0.1.seconds, slideDirection))
        }
    }

    private fun imageView(params: ViewImageParams) = imageview {
        fitWidth = screenBounds.width * 3 / 4
        fitHeight = screenBounds.height * 3 / 4
        isPreserveRatio = true

        if (params.imageUrl.isNotBlank()) {
            imageProperty().bind(commonOps.fetchImage(params.imageUrl, persist = true))
        } else {
            image = null
        }
    }

    class Style : Stylesheet() {
        companion object {
            val arrowLeftContainer by cssclass()
            val arrowLeft by cssclass()
            val arrowRightContainer by cssclass()
            val arrowRight by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            arrowLeftContainer {
                padding = box(20.px)
            }
            arrowLeft {
                backgroundColor = multi(Colors.transparentWhite)
                and(hover) {
                    backgroundColor = multi(Color.WHITE)
                }
            }
            arrowRightContainer {
                padding = box(20.px)
            }
            arrowRight {
                backgroundColor = multi(Colors.transparentWhite)
                and(hover) {
                    backgroundColor = multi(Color.WHITE)
                }
            }
        }
    }
}
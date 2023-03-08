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

package com.gitlab.ykrasik.gamedex.javafx.control

import com.gitlab.ykrasik.gamedex.app.api.util.broadcastFlow
import com.gitlab.ykrasik.gamedex.javafx.importStylesheetSafe
import com.gitlab.ykrasik.gamedex.javafx.theme.Colors
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.setAll
import javafx.beans.binding.Bindings.isNotEmpty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import tornadofx.*

/**
 * User: ykrasik
 * Date: 04/05/2019
 * Time: 20:51
 */
class OverlayPane : StackPane() {
    private val log = logger()

    @Suppress("UNCHECKED_CAST")
    private val visibleLayers = children as ObservableList<OverlayLayerImpl>

    private val savedLayers = mutableListOf<OverlayLayerImpl>()

    init {
        visibleProperty().bind(isNotEmpty(visibleLayers))
        addEventHandler(MouseEvent.MOUSE_CLICKED) { it.consume() }
        addEventHandler(KeyEvent.KEY_PRESSED) { it.consume() }
        visibleLayers.onChange {
            val preLast = visibleLayers.getOrNull(visibleLayers.size - 2)
            preLast?.active?.set(false)

            val last = visibleLayers.lastOrNull()
            last?.active?.set(true)

            // Don't steal focus from already focused nodes.
            if (last?.hasFocus == false) {
                last.view.root.requestFocus()
            }
        }
    }

    private val OverlayLayerImpl.hasFocus: Boolean
        get() {
            var checkedNode = scene.focusOwner
            while (checkedNode != null) {
                if (checkedNode === view.root) return true
                checkedNode = checkedNode.parent
            }
            return false
        }

    fun show(view: View, customizeOverlay: OverlayLayerImpl.() -> Unit): OverlayLayer =
        show(OverlayLayerImpl(view).also(customizeOverlay))

    private fun show(layer: OverlayLayerImpl) = layer.apply {
        log.trace("Showing overlay: $layer")
        layer.content.isVisible = true
        layer.content.children += layer.view.root
        visibleLayers += layer
        layer.fillTransition(0.2.seconds, from = invisibleColor, to = visibleColor)
    }

    fun isShowing(view: View): Boolean = visibleLayers.any { it.view === view }

    fun hide(view: View) = hide(checkNotNull(visibleLayers.findLast { it.view === view }) { "View not showing: $view" }, dispose = true)

    private fun hide(layer: OverlayLayerImpl, dispose: Boolean) {
        if (layer.isHiding) return
        layer.isHiding = true
        log.trace("Hiding overlay: $layer")

        // Immediately hide the content
        layer.content.isVisible = false
        layer.content.children -= layer.view.root

        layer.fillTransition(0.2.seconds, from = visibleColor, to = invisibleColor) {
            setOnFinished {
                visibleLayers -= layer
                layer.isHiding = false
                if (dispose) layer.close()
            }
        }
    }

    fun forceHideAll() = visibleLayers.forEach { it.requestHide() }

    fun saveAndClear() {
        savedLayers.setAll(visibleLayers.filter { !it.isHiding })
        visibleLayers.toList().forEach { hide(it, dispose = false) }
    }

    fun restoreSaved() {
        savedLayers.forEach { show(it) }
        savedLayers.clear()
    }

    class OverlayLayerImpl(val view: View) : StackPane(), OverlayLayer {
        override val active = SimpleBooleanProperty(false)
        var isActive by active

        override val hiding = SimpleBooleanProperty(false)
        var isHiding by hiding

        override val hideRequests = broadcastFlow<Unit>()

        init {
            // This stackPane extends over the whole screen and will be darkened.
            // It catches clicks outside of the content area and hides the content.
            useMaxSize = true
            addEventHandler(MouseEvent.MOUSE_RELEASED) { e ->
                if (e.source == this && e.target == this) {
                    requestHide()
                }
                e.consume()
            }
            addEventFilter(KeyEvent.KEY_PRESSED) { e ->
                if (e.code == KeyCode.ESCAPE) {
                    requestHide()
                    e.consume()
                }
            }
            addEventHandler(KeyEvent.KEY_PRESSED) { it.consume() }
        }

        val content = stackpane {
            // This stackPane will wrap the content,
            // it's here mostly to prevent the parent stackPane from resizing the content to the whole screen.
            maxWidth = Region.USE_PREF_SIZE
            maxHeight = Region.USE_PREF_SIZE
            addClass(Style.overlayContent)
            clipRectangle(arc = 20)
            addEventHandler(MouseEvent.MOUSE_CLICKED) { it.consume() }
        }

        fun requestHide() {
            hideRequests.trySendBlocking(Unit).getOrThrow()
        }

        fun close() {
            hideRequests.close()
        }

        override fun toString() = view.toString()
    }

    private companion object {
        val visibleColor: Color = Color.rgb(0, 0, 0, 0.7)
        val invisibleColor: Color = Color.rgb(0, 0, 0, 0.0)
    }

    class Style : Stylesheet() {
        companion object {
            val overlayContent by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            overlayContent {
                backgroundColor = multi(Colors.cloudyKnoxville)
            }
        }
    }
}

interface OverlayLayer {
    val active: ReadOnlyBooleanProperty
    val hiding: ReadOnlyBooleanProperty

    val hideRequests: Flow<Unit>
}

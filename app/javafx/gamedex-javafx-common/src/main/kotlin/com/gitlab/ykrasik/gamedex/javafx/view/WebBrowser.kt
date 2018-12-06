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

package com.gitlab.ykrasik.gamedex.javafx.view

import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.jfxButton
import javafx.beans.property.Property
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.web.WebView
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/06/2017
 * Time: 22:29
 */
class WebBrowser : Fragment() {
    private var webView: WebView = webview()

    private val fullscreenBrowser = StandaloneBrowserFragment()
    private lateinit var prevParent: Pane

    override val root = borderpane {
        vgrow = Priority.ALWAYS
        center {
            add(webView)
        }
        top {
            defaultHbox {
                paddingAll = 5.0

                jfxButton(graphic = Icons.arrowLeft) {
                    addClass(CommonStyle.thinBorder)
                    enableWhen { canNavigate(back = true) }
                    action { navigate(back = true) }
                }
                jfxButton(graphic = Icons.arrowRight) {
                    addClass(CommonStyle.thinBorder)
                    enableWhen { canNavigate(back = false) }
                    action { navigate(back = false) }
                }
                spacer()
                jfxButton {
                    addClass(CommonStyle.thinBorder)
                    graphicProperty().bind(fullscreenBrowser.isDockedProperty.objectBinding { if (it == true) Icons.exitFullscreen else Icons.fullscreen })
                    action { toggleStandalone() }
                }
            }
        }
    }

    // TODO: Find a way to clear browsing history on stop.
    // TODO: Don't stop if in standalone mode.
    fun load(url: String?) = webView.engine.load(url)

    private fun toggleStandalone() {
        if (fullscreenBrowser.isDocked) {
            fullscreenBrowser.close()
        } else {
            openStandalone()
        }
    }

    private fun openStandalone() {
        prevParent = root.parent as Pane
        fullscreenBrowser.openWindow(block = true, owner = null)
        prevParent.children += root
    }

    private fun canNavigate(back: Boolean): Property<Boolean> {
        val history = webView.engine.history
        val entries = history.entries
        return history.currentIndexProperty().map { i ->
            val currentIndex = i!!.toInt()
            entries.size > 1 && (if (back) currentIndex > 0 else currentIndex < entries.size - 1)
        }
    }

    private fun navigate(back: Boolean) = webView.engine.history.go(if (back) -1 else 1)

    private inner class StandaloneBrowserFragment : Fragment() {
        override val root = stackpane {
            minWidth = screenBounds.width * 2 / 3
            minHeight = screenBounds.height * 2 / 3
        }

        init {
            titleProperty.bind(webView.engine.locationProperty())
        }

        override fun onDock() {
            modalStage!!.isMaximized = true
            root.children += this@WebBrowser.root
        }
    }
}
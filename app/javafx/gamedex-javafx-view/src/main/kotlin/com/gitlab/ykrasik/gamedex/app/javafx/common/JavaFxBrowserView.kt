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

package com.gitlab.ykrasik.gamedex.app.javafx.common

import com.gitlab.ykrasik.gamedex.app.api.web.BrowserView
import com.gitlab.ykrasik.gamedex.javafx.control.clipRectangle
import com.gitlab.ykrasik.gamedex.javafx.control.jfxButton
import com.gitlab.ykrasik.gamedex.javafx.control.jfxTextField
import com.gitlab.ykrasik.gamedex.javafx.control.prettyToolbar
import com.gitlab.ykrasik.gamedex.javafx.screenBounds
import com.gitlab.ykrasik.gamedex.javafx.theme.Colors
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.color
import com.gitlab.ykrasik.gamedex.javafx.theme.confirmButton
import com.gitlab.ykrasik.gamedex.javafx.typesafeBooleanBinding
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import javafx.beans.value.ObservableValue
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.web.WebView
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/06/2017
 * Time: 22:29
 */
class JavaFxBrowserView : PresentableView("Browser", Icons.web), BrowserView {
    private val webView: WebView = webview()

    private val fullscreenBrowser = StandaloneBrowserFragment()
    private lateinit var prevParent: Pane

    init {
        register()
    }

    override val root = borderpane {
        vgrow = Priority.ALWAYS
        prefWidth = screenBounds.width * 9 / 10
        prefHeight = screenBounds.height * 9 / 10
        clipRectangle(arc = 20)
        top = prettyToolbar {
            jfxButton(graphic = Icons.arrowLeft) {
                enableWhen { canNavigate(back = true) }
                action { navigate(back = true) }
            }
            jfxButton(graphic = Icons.arrowRight) {
                enableWhen { canNavigate(back = false) }
                action { navigate(back = false) }
            }
            val textField = jfxTextField {
                useMaxSize = true
                hgrow = Priority.ALWAYS
                webView.engine.locationProperty().onChange {
                    text = it
                }
                setOnAction {
                    load(text)
                    this@prettyToolbar.requestFocus()
                }
            }
            confirmButton(graphic = Icons.arrowRightCircle.color(Colors.green), isToolbarButton = false) {
                action { load(textField.text) }
            }
            jfxButton {
                graphicProperty().bind(fullscreenBrowser.isDockedProperty.objectBinding { if (it == true) Icons.exitFullscreen else Icons.fullscreen })
                action { toggleStandalone() }
            }
        }
        center = stackpane {
            add(webView)
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

    private fun canNavigate(back: Boolean): ObservableValue<Boolean> {
        val history = webView.engine.history
        val entries = history.entries
        return history.currentIndexProperty().typesafeBooleanBinding { i ->
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
            root.children += this@JavaFxBrowserView.root
        }
    }
}
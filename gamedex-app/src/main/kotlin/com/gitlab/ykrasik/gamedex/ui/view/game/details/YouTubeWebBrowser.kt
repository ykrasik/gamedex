package com.gitlab.ykrasik.gamedex.ui.view.game.details

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ui.map
import com.gitlab.ykrasik.gamedex.ui.screenBounds
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import javafx.beans.property.Property
import javafx.scene.layout.Pane
import javafx.scene.web.WebView
import tornadofx.*
import java.net.URLEncoder

/**
 * User: ykrasik
 * Date: 09/06/2017
 * Time: 22:29
 */
class YouTubeWebBrowser : Fragment() {
    private var webView: WebView by singleAssign()

    private val standalone by lazy { StandaloneBrowserFragment() }
    private lateinit var prevParent: Pane

    override val root = borderpane {
        center {
            webView = webview()
        }
        top {
            hbox(spacing = 10.0) {
                paddingAll = 5.0

                button(graphic = Theme.Icon.arrowLeft(18.0)) {
                    enableWhen { canNavigate(back = true) }
                    setOnAction { navigate(back = true) }
                }
                button(graphic = Theme.Icon.arrowRight(18.0)) {
                    enableWhen { canNavigate(back = false) }
                    setOnAction { navigate(back = false) }
                }
                spacer()
                button(graphic = Theme.Icon.maximize(18.0)) {
                    setOnAction { toggleStandalone() }
                }
            }
        }
    }

    fun searchYoutube(game: Game) {
        val search = URLEncoder.encode("${game.name} ${game.platform} gameplay", "utf-8")
        val url = "https://www.youtube.com/results?search_query=$search"
        load(url)
    }

    fun load(url: String) = webView.engine.load(url)

    // TODO: Find a way to clear browsing history on stop.
    // TODO: Don't stop if in standalone mode.
    fun stop() {
        webView.engine.load(null)
    }

    private fun toggleStandalone() {
        if (standalone.isDocked) {
            standalone.close()
        } else {
            openStandalone()
        }
    }

    private fun openStandalone() {
        prevParent = root.parent as Pane
        standalone.openWindow(block = true, owner = null)
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

    inner class StandaloneBrowserFragment : Fragment() {
        override val root = stackpane {
            minWidth = screenBounds.width * 2 / 3
            minHeight = screenBounds.height * 2 / 3
        }

        init {
            titleProperty.bind(webView.engine.locationProperty())
        }

        override fun onDock() {
            modalStage!!.isMaximized = true
            root.children += this@YouTubeWebBrowser.root
        }
    }
}
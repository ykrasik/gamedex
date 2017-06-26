package com.gitlab.ykrasik.gamedex.ui.view.game.details

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ui.map
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import javafx.beans.property.Property
import javafx.scene.web.WebView
import tornadofx.*
import java.net.URLEncoder

/**
 * User: ykrasik
 * Date: 09/06/2017
 * Time: 22:29
 */
class WebBrowser : Fragment() {
    private var webView: WebView by singleAssign()

    override val root = borderpane {
        center {
            webView = webview()
        }
        top {
            hbox(spacing = 10.0) {
                paddingBottom = 5.0

                button(graphic = Theme.Icon.arrowLeft(18.0)) {
                    enableWhen { canNavigate(back = true) }
                    setOnAction { navigate(back = true) }
                }
                button(graphic = Theme.Icon.arrowRight(18.0)) {
                    enableWhen { canNavigate(back = false) }
                    setOnAction { navigate(back = false) }
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
    fun stop() {
        webView.engine.load(null)
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
}
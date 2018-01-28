package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.core.Filter
import com.gitlab.ykrasik.gamedex.ui.jfxButton
import com.gitlab.ykrasik.gamedex.ui.popoverContextMenu
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import difflib.Chunk
import difflib.Delta
import javafx.event.EventTarget
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import javafx.scene.text.TextFlow
import tornadofx.*

/**
 * User: ykrasik
 * Date: 24/06/2017
 * Time: 18:52
 */
class DiffResultFragment(diff: Filter.NameDiff.GameNameFolderDiff, game: Game) : Fragment() {
    private val gameController: GameController by di()

    override val root = form {
        addClass(CommonStyle.centered)
        fieldset {
            field("Expected") { render(diff.expectedName, diff.patch.deltas) { it.revised } }
            field("Actual") { render(diff.actualName, diff.patch.deltas) { it.original } }
        }

        popoverContextMenu {
            jfxButton("Rename to Expected", Theme.Icon.folder()) {
                setOnAction {
                    gameController.renameFolder(game, diff.expectedName)
                }
            }
            // TODO: Add a 'search only this provider' option
        }
    }

    private fun EventTarget.render(source: String, deltas: List<Delta<Char>>, chunkExtractor: (Delta<Char>) -> Chunk<Char>) = textflow {
        var currentIndex = 0
        deltas.sortedBy { chunkExtractor(it).position }.forEach { delta ->
            val chunk = chunkExtractor(delta)

            match(source.substring(currentIndex, chunk.position))
            currentIndex = chunk.position

            diff(source.substring(currentIndex, currentIndex + chunk.size()), delta.type)
            currentIndex += chunk.size()
        }
        match(source.substring(currentIndex, source.length))
    }

    private fun TextFlow.match(text: String) = label(text) { addClass(Style.match) }

    private fun TextFlow.diff(text: String, type: Delta.TYPE) = label(text) {
        addClass(Style.match, Style.diff, when (type) {
            Delta.TYPE.CHANGE -> Style.change
            Delta.TYPE.DELETE -> Style.delete
            Delta.TYPE.INSERT -> Style.insert
        })
    }

    class Style : Stylesheet() {
        companion object {
            val match by cssclass()
            val diff by cssclass()
            val change by cssclass()
            val insert by cssclass()
            val delete by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            match {
                fontSize = 18.px
                fontFamily = "Monospaced"
            }
            diff {
                fontWeight = FontWeight.BOLD
            }
            change {
                backgroundColor = multi(Color.ORANGE)
            }
            insert {
                backgroundColor = multi(Color.GREEN)
            }
            delete {
                backgroundColor = multi(Color.INDIANRED)
            }
        }
    }
}
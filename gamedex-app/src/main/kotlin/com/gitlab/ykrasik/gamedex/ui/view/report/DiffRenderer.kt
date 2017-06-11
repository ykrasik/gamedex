package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.core.GameNameFolderDiff
import difflib.Chunk
import difflib.Delta
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import javafx.scene.text.TextFlow
import tornadofx.*

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 15:42
 */
object DiffRenderer {
    fun renderActual(diff: GameNameFolderDiff): TextFlow = render(diff.actual, diff.patch.deltas) { it.original }
    fun renderExpected(diff: GameNameFolderDiff): TextFlow = render(diff.expected, diff.patch.deltas) { it.revised }

    private fun render(source: String, deltas: List<Delta<Char>>, chunkExtractor: (Delta<Char>) -> Chunk<Char>) = TextFlow().apply {
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
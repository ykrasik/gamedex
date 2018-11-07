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

package com.gitlab.ykrasik.gamedex.app.javafx.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanRenameMoveGame
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.Theme
import com.gitlab.ykrasik.gamedex.javafx.importStylesheetSafe
import com.gitlab.ykrasik.gamedex.javafx.jfxButton
import com.gitlab.ykrasik.gamedex.javafx.view.InstallableContextMenu
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
    private val contextMenu: DiffResultContextMenu by inject()

    override val root = form {
        addClass(CommonStyle.centered)
        fieldset {
            field("Expected") { render(diff.expectedName, diff.patch.deltas) { it.revised } }
            field("Actual") { render(diff.actualName, diff.patch.deltas) { it.original } }
        }

        contextMenu.install(this) { game to diff }
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
        addClass(
            Style.match, Style.diff, when (type) {
                Delta.TYPE.CHANGE -> Style.change
                Delta.TYPE.DELETE -> Style.delete
                Delta.TYPE.INSERT -> Style.insert
            }
        )
    }

    class Style : Stylesheet() {
        companion object {
            val match by cssclass()
            val diff by cssclass()
            val change by cssclass()
            val insert by cssclass()
            val delete by cssclass()

            init {
                importStylesheetSafe(Style::class)
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

class DiffResultContextMenu : InstallableContextMenu<Pair<Game, Filter.NameDiff.GameNameFolderDiff>>(), ViewCanRenameMoveGame {
    override val renameMoveGameActions = channel<Pair<Game, String?>>()

    init {
        viewRegistry.onCreate(this)
    }

    override val root = vbox {
        jfxButton("Rename to Expected", Theme.Icon.folder()) {
            eventOnAction(renameMoveGameActions) { data.first to data.second.expectedName }
        }
        // TODO: Add a 'search only this provider' option
    }
}
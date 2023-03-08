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

package com.gitlab.ykrasik.gamedex.app.javafx.game

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.RawGameDataView
import com.gitlab.ykrasik.gamedex.app.api.util.broadcastFlow
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.codeArea
import com.gitlab.ykrasik.gamedex.javafx.control.prettyToolbar
import com.gitlab.ykrasik.gamedex.javafx.control.virtualizedScrollPane
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.acceptButton
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import org.fxmisc.richtext.CodeArea
import tornadofx.*

/**
 * User: ykrasik
 * Date: 01/06/2020
 * Time: 02:19
 */
class JavaFxRawGameDataView : PresentableView(icon = Icons.json), RawGameDataView {
    override val game = viewMutableStateFlow(Game.Null, debugName = "game")
    override val rawGameData = mutableStateFlow(emptyMap<String, Any>(), debugName = "rawGameData")

    override val acceptActions = broadcastFlow<Unit>()

    private var indent = 0
    private val renderedIndent get() = " ".repeat(indent)

    override val root = borderpane {
        top = prettyToolbar {
            spacer()
            acceptButton {
                isCancelButton = true
                action(acceptActions)
            }
        }
        center = virtualizedScrollPane(codeArea {
            addClass(Style.rawGameDataView)
            isWrapText = true

            rawGameData.onChange {
                clear()

                // decIndent because the first thing a map does when it is rendered is incIndent
                decIndent()
                render(it, indentValue = false)
                incIndent()
            }
        }) {
            minWidth = screenBounds.width * 5 / 6
            minHeight = screenBounds.height * 5 / 6
        }
    }

    init {
        titleProperty.bind(game.property.typesafeStringBinding { it.name })

        register()
    }

    private fun incIndent() {
        indent += 2
    }

    private fun decIndent() {
        indent -= 2
    }

    private fun CodeArea.render(value: Any?, indentValue: Boolean, index: Int? = null) {
        fun newLine() = append("\n", "")

        when (value) {
            is Map<*, *> -> {
                if (value.isNotEmpty()) {
                    if (index != null) append("$renderedIndent$index:", Style.key.render().drop(1))
                    newLine()

                    incIndent()
                    value.forEach { (key, value) ->
                        append("$renderedIndent$key: ", Style.key.render().drop(1))
                        render(value, indentValue = false)
                    }
                    deletePreviousChar()
                    decIndent()
                } else {
                    append("{}", Style.`null`.render().drop(1))
                }
            }
            is List<*> -> {
                if (value.isNotEmpty()) {
                    newLine()

                    incIndent()
                    value.forEachIndexed { i, it ->
                        render(it, indentValue = true, index = i)
                    }
                    deletePreviousChar()
                    decIndent()
                } else {
                    append("[]", Style.`null`.render().drop(1))
                }
            }
            else -> {
                if (index != null) {
                    append("${if (indentValue) renderedIndent else ""}$index: ", Style.key.render().drop(1))
                    append(value.toString(), if (value == null) Style.`null`.render().drop(1) else "")
                } else {
                    append("${if (indentValue) renderedIndent else ""}$value", if (value == null) Style.`null`.render().drop(1) else "")
                }
            }
        }
        newLine()
    }

    class Style : Stylesheet() {
        companion object {
            val rawGameDataView by cssclass()
            val key by cssclass()
            val `null` by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            rawGameDataView {
                padding = box(vertical = 0.px, horizontal = 10.px)
                fontFamily = "monospace"
            }

            key {
                fontWeight = FontWeight.BOLD
                fill = Color.PURPLE
            }

            `null` {
                fontWeight = FontWeight.BOLD
                fill = Color.BROWN
            }
        }
    }
}
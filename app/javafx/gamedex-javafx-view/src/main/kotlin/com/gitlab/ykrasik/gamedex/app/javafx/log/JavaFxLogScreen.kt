/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.javafx.log

import com.gitlab.ykrasik.gamedex.app.api.log.*
import com.gitlab.ykrasik.gamedex.javafx.control.enumComboMenu
import com.gitlab.ykrasik.gamedex.javafx.control.jfxCheckBox
import com.gitlab.ykrasik.gamedex.javafx.control.jfxListView
import com.gitlab.ykrasik.gamedex.javafx.importStylesheetSafe
import com.gitlab.ykrasik.gamedex.javafx.sortedFiltered
import com.gitlab.ykrasik.gamedex.javafx.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.size
import com.gitlab.ykrasik.gamedex.javafx.userMutableState
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import com.jfoenix.controls.JFXListCell
import javafx.scene.input.KeyCombination
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import tornadofx.*
import java.io.PrintWriter
import java.io.StringWriter

/**
 * User: ykrasik
 * Date: 28/04/2017
 * Time: 11:14
 */
class JavaFxLogScreen : PresentableScreen("Log", Icons.book), ViewWithLogEntries, ViewCanChangeLogLevel, ViewCanChangeLogTail {
    override val entries = mutableListOf<LogEntry>().observable().sortedFiltered()

    override var level = userMutableState(LogLevel.Info)
    override var logTail = userMutableState(false)

    init {
        entries.predicate = { entry -> entry.level.canLog(level.value) }
        level.onChange { entries.refilter() }
//        observableEntries.predicateProperty.bind(levelProperty.toPredicateF { level, entry ->
//            entry.level.toLevel().isGreaterOrEqual(level!!.toLevel())
//        })

        register()
    }

    override fun HBox.buildToolbar() {
        enumComboMenu(level.property, graphic = { it.icon }).apply {
            addClass(CommonStyle.toolbarButton)
        }
        jfxCheckBox(logTail.property, "Tail")
    }

    override val root = jfxListView(entries) {
        addClass(Style.logView)

        setCellFactory {
            object : JFXListCell<LogEntry>() {
                init {
//                    addClass(CommonStyle.jfxHoverable)
                    contextmenu {
                        item("Copy to Clipboard", KeyCombination.keyCombination("ctrl+c")).action {
                            clipboard.putString(item.message)
                        }
                    }
                }

                override fun updateItem(item: LogEntry?, empty: Boolean) {
                    super.updateItem(item, empty)

                    toggleClass(Style.trace, false)
                    toggleClass(Style.debug, false)
                    toggleClass(Style.info, false)
                    toggleClass(Style.warn, false)
                    toggleClass(Style.error, false)

                    if (item == null || empty) {
                        text = null
                        graphic = null
                        return
                    }

                    val message = if (item.throwable != null) {
                        val sw = StringWriter()
                        item.throwable!!.printStackTrace(PrintWriter(sw))
                        sw.toString()
                    } else {
                        item.message
                    }
                    text = "${item.timestamp.toString("HH:mm:ss.SSS")} [${item.loggerName}] $message"
                    graphic = item.level.icon.size(20)

                    when (item.level) {
                        LogLevel.Trace -> toggleClass(Style.trace, true)
                        LogLevel.Debug -> toggleClass(Style.debug, true)
                        LogLevel.Info -> toggleClass(Style.info, true)
                        LogLevel.Warn -> toggleClass(Style.warn, true)
                        LogLevel.Error -> toggleClass(Style.error, true)
                    }
                }
            }
        }

        entries.onChange {
            if (logTail.value) {
                scrollTo(items.size)
            }
        }
    }

    private val LogLevel.icon
        get() = when (this) {
            LogLevel.Trace -> Icons.logTrace
            LogLevel.Debug -> Icons.logDebug
            LogLevel.Info -> Icons.logInfo
            LogLevel.Warn -> Icons.logWarn
            LogLevel.Error -> Icons.logError
        }

    class Style : Stylesheet() {
        companion object {
            val logView by cssclass()

            val trace by csspseudoclass()
            val debug by csspseudoclass()
            val info by csspseudoclass()
            val warn by csspseudoclass()
            val error by csspseudoclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            logView {
                listCell {
                    backgroundColor = multi(Color.WHITE) // removes alternating list gray cells.

                    and(trace) {
                        textFill = Icons.logTrace.iconColor
                    }
                    and(debug) {
                        textFill = Icons.logDebug.iconColor
                    }
                    and(info) {
                        textFill = Icons.logInfo.iconColor
                    }
                    and(warn) {
                        textFill = Icons.logWarn.iconColor
                    }
                    and(error) {
                        textFill = Icons.logError.iconColor
                    }
                    and(selected) {
                        backgroundColor = multi(Color.LIGHTBLUE)
                    }
                }
            }
        }
    }
}
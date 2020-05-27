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

package com.gitlab.ykrasik.gamedex.app.javafx.log

import com.gitlab.ykrasik.gamedex.app.api.log.*
import com.gitlab.ykrasik.gamedex.javafx.control.codeArea
import com.gitlab.ykrasik.gamedex.javafx.control.enumComboMenu
import com.gitlab.ykrasik.gamedex.javafx.control.prettyToolbar
import com.gitlab.ykrasik.gamedex.javafx.control.virtualizedScrollPane
import com.gitlab.ykrasik.gamedex.javafx.importStylesheetSafe
import com.gitlab.ykrasik.gamedex.javafx.screenBounds
import com.gitlab.ykrasik.gamedex.javafx.settableList
import com.gitlab.ykrasik.gamedex.javafx.theme.Colors
import com.gitlab.ykrasik.gamedex.javafx.theme.GameDexStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.javafx.viewMutableStateFlow
import javafx.scene.text.FontWeight
import tornadofx.*
import java.io.PrintWriter
import java.io.StringWriter

/**
 * User: ykrasik
 * Date: 28/04/2017
 * Time: 11:14
 */
class JavaFxLogView : PresentableView("Log", Icons.book),
    LogView,
    ViewCanChangeLogLevel {
    override val entries = settableList<LogEntry>()

    override val level = viewMutableStateFlow(LogLevel.Info, debugName = "level")

    private val baseStyles = LogLevel.values().map { level ->
        level to when (level) {
            LogLevel.Trace -> Style.trace
            LogLevel.Debug -> Style.debug
            LogLevel.Info -> Style.info
            LogLevel.Warn -> Style.warn
            LogLevel.Error -> Style.error
        }.render().drop(1)
    }.toMap()

    private val entriesToPositions = mutableMapOf<LogEntryId, LogEntryPosition>()
    private var totalRemoved = 0

    private val textArea = codeArea {
        addClass(Style.logView)
        isWrapText = true

//        val numberFactory = LineNumberFactory.get(this)
//        paragraphGraphicFactory = IntFunction<Node> { line ->
//            val hbox = HBox(numberFactory.apply(line))
//            hbox.alignment = Pos.CENTER_LEFT
//            hbox
//        }
    }

    init {
        level.onChange {
            initLogLevel()
        }

        entries.onChange { change ->
            while (change.next()) {
                change.addedSubList.forEach(::addEntry)
                change.removed.forEach(::removeEntry)
            }
        }

        register()
    }

    override val root = borderpane {
        top = prettyToolbar {
            enumComboMenu(level.property, text = LogLevel::displayName, graphic = { it.icon }).apply {
                addClass(GameDexStyle.toolbarButton)
            }
        }
        center = virtualizedScrollPane(textArea) {
            minWidth = screenBounds.width * 5 / 6
            minHeight = screenBounds.height * 5 / 6
        }
    }

    private fun initLogLevel() {
        entriesToPositions.clear()
        totalRemoved = 0
        textArea.clear()
        entries.forEach(::addEntry)
    }

    private fun addEntry(entry: LogEntry) {
        if (!level.v.canLog(entry)) return

        val baseStyle = baseStyles.getValue(entry.level)

        val startIndex = textArea.length + totalRemoved
        var textLength = 0

        fun append(text: String, extraStyle: String? = null, withBraces: Boolean = true, withSpace: Boolean = true, withNewLine: Boolean = false) {
            if (text.isEmpty()) return

            val sw = StringWriter(text.length + (if (withBraces) 2 else 0) + (if (withSpace) 1 else 0) + (if (withNewLine) 1 else 0))
            if (withBraces) sw.append('[')
            sw.append(text)
            if (withBraces) sw.append(']')
            if (withSpace) sw.append(' ')
            if (withNewLine) sw.append('\n')
            val message = sw.toString()

            textArea.append(message, listOf(baseStyle, extraStyle))
            textLength += message.length
        }

        append(entry.formattedTimestamp, withBraces = false)
        append(entry.formattedLevel, Style.level.render().drop(1), withBraces = false)
        append(entry.formattedLoggerName, Style.loggerName.render().drop(1))
        append(entry.formattedThreadName, Style.threadName.render().drop(1))
        append(entry.formattedMessage, Style.message.render().drop(1), withBraces = false, withSpace = false, withNewLine = true)

        val endIndex = startIndex + textLength
        entriesToPositions[entry.id] = LogEntryPosition(startIndex, endIndex)
    }

    private fun removeEntry(entry: LogEntry) {
        if (!level.v.canLog(entry)) return

        val (startIndex, endIndex) = entriesToPositions.remove(entry.id)!!
        textArea.deleteText(startIndex - totalRemoved, endIndex - totalRemoved)
        totalRemoved += endIndex - startIndex
    }

    private val LogEntry.formattedMessage
        get() = throwable?.let { throwable ->
            val sw = StringWriter()
            sw.appendln(message)
            throwable.printStackTrace(PrintWriter(sw))
            sw.toString()
        } ?: message

    private val LogEntry.formattedTimestamp get() = timestamp.toString("HH:mm:ss")
    private val LogEntry.formattedLevel
        get() = when (level) {
            LogLevel.Info, LogLevel.Warn -> "$level "
            else -> level.toString()
        }
    private val LogEntry.formattedThreadName get() = threadName.split('@').let { if (it.size > 1) it[1] else it[0] }
    private val LogEntry.formattedLoggerName get() = loggerName

    private val LogLevel.icon
        get() = when (this) {
            LogLevel.Trace -> Icons.logTrace
            LogLevel.Debug -> Icons.logDebug
            LogLevel.Info -> Icons.logInfo
            LogLevel.Warn -> Icons.logWarn
            LogLevel.Error -> Icons.logError
        }

    private data class LogEntryPosition(val startIndex: Int, val endIndex: Int)

    class Style : Stylesheet() {
        companion object {
            val logView by cssclass()

            val level by cssclass()
            val threadName by cssclass()
            val loggerName by cssclass()
            val message by cssclass()

            val trace by cssclass()
            val debug by cssclass()
            val info by cssclass()
            val warn by cssclass()
            val error by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            logView {
                padding = box(vertical = 0.px, horizontal = 10.px)
            }

            level {
            }
            threadName {
                fill = Colors.green
                fontWeight = FontWeight.BOLD
            }
            loggerName {
                fill = Colors.niceBlueDarker
                fontWeight = FontWeight.BOLD
            }
            message {
                fontWeight = FontWeight.BLACK
            }

            trace {
                opacity = 0.5
            }
            debug {
                opacity = 0.75
            }
            info {
            }
            warn {
                fill = Colors.orange
            }
            error {
                fill = Colors.red
                fontWeight = FontWeight.BOLD
            }
        }
    }
}
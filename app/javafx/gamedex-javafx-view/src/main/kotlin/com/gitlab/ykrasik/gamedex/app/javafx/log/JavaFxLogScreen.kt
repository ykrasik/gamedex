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

package com.gitlab.ykrasik.gamedex.app.javafx.log

import ch.qos.logback.classic.Level
import com.gitlab.ykrasik.gamedex.app.api.log.LogEntry
import com.gitlab.ykrasik.gamedex.app.api.log.LogView
import com.gitlab.ykrasik.gamedex.app.api.presenters
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableGamedexScreen
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ListCell
import javafx.scene.control.ToolBar
import javafx.scene.input.KeyCombination
import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 28/04/2017
 * Time: 11:14
 */
class JavaFxLogScreen : PresentableGamedexScreen<LogView.Event>("Log", Theme.Icon.book()), LogView {
    private val observableEntries = mutableListOf<LogEntry>().observable().sortedFiltered()
    override var entries by InitOnceListObservable(observableEntries)

    private val levelProperty = SimpleStringProperty().eventOnChange(LogView.Event::LevelChanged)
    override var level by levelProperty

    private val logTailProperty = SimpleBooleanProperty(false).eventOnChange(LogView.Event::LogTailChanged)
    override var logTail by logTailProperty

    init {
        presenters.logPresenter.present(this)

        observableEntries.predicate = { entry -> entry.level.toLevel().isGreaterOrEqual(level.toLevel()) }
        levelProperty.onChange { observableEntries.refilter() }
//        observableEntries.predicateProperty.bind(levelProperty.toPredicateF { level, entry ->
//            entry.level.toLevel().isGreaterOrEqual(level!!.toLevel())
//        })
    }

    override fun ToolBar.constructToolbar() {
        header("Level").labelFor =
            popoverComboMenu(
                possibleItems = listOf(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR).map { it.levelStr.toLowerCase().capitalize() },
                selectedItemProperty = levelProperty
            ).apply {
                minWidth = 60.0
            }
        header("Tail").labelFor =
            jfxCheckBox(logTailProperty)
    }

    override val root = listview(observableEntries) {
        addClass(Style.logView)

        setCellFactory {
            object : ListCell<LogEntry>() {
                init {
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
                        return
                    }

                    text = "${item.timestamp.toString("HH:mm:ss.SSS")} [${item.loggerName}] ${item.message}"

                    when (item.level.toLevel()) {
                        Level.TRACE -> toggleClass(Style.trace, true)
                        Level.DEBUG -> toggleClass(Style.debug, true)
                        Level.INFO -> toggleClass(Style.info, true)
                        Level.WARN -> toggleClass(Style.warn, true)
                        Level.ERROR -> toggleClass(Style.error, true)
                    }
                }
            }
        }

        observableEntries.onChange {
            if (logTail) {
                scrollTo(items.size)
            }
        }
    }

    private fun String.toLevel() = Level.toLevel(this)

    class Style : Stylesheet() {
        companion object {
            val logView by cssclass()

            val trace by csspseudoclass()
            val debug by csspseudoclass()
            val info by csspseudoclass()
            val warn by csspseudoclass()
            val error by csspseudoclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            logView {
                listCell {
                    backgroundColor = multi(Color.WHITE) // removes alternating list gray cells.

                    and(trace) {
                        textFill = Color.LIGHTGRAY
                    }
                    and(debug) {
                        textFill = Color.GRAY
                    }
                    and(warn) {
                        textFill = Color.ORANGE
                    }
                    and(error) {
                        textFill = Color.RED
                    }
                    and(selected) {
                        backgroundColor = multi(Color.LIGHTBLUE)
                    }
                }
            }
        }
    }
}
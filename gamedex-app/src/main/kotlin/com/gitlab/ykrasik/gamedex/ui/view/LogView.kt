package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.preferences.UserPreferences
import com.gitlab.ykrasik.gamedex.ui.enumComboBox
import com.gitlab.ykrasik.gamedex.util.*
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
class LogView : GamedexView("Log") {
    private val userPreferences: UserPreferences by di()

    private val logItems = SortedFilteredList(Log.entries)

    override fun ToolBar.constructToolbar() {
        enumComboBox(userPreferences.logFilterLevelProperty)
        togglebutton("Tail") {
            userPreferences.logTailProperty.bindBidirectional(selectedProperty())
        }
    }

    // TODO: TableView?
    override val root = listview(logItems) {
        addClass(Style.logView)

        setCellFactory {
            object : ListCell<LogEntry>() {
                init {
                    contextmenu {
                        menuitem("Copy to Clipboard", KeyCombination.keyCombination("ctrl+c")) {
                            clipboard.putString(item.message)
                        }
                    }
                }

                override fun updateItem(item: LogEntry?, empty: Boolean) {
                    super.updateItem(item, empty)

                    toggleClass(Style.debug, false)
                    toggleClass(Style.info, false)
                    toggleClass(Style.warn, false)
                    toggleClass(Style.error, false)

                    if (item == null || empty) {
                        text = null
                        return
                    }

                    // TODO: Color different context differently?
                    text = "${item.timestamp.toString("HH:mm:ss.SSS")} [${item.context}] ${item.message}"

                    when (item.level) {
                        LogLevel.debug -> toggleClass(Style.debug, true)
                        LogLevel.info -> toggleClass(Style.info, true)
                        LogLevel.warn -> toggleClass(Style.warn, true)
                        LogLevel.error -> toggleClass(Style.error, true)
                    }
                }
            }
        }

        logItems.onChange {
            if (userPreferences.logTail) {
                scrollTo(items.size)
            }
        }
    }

    init {
        globalLogLevel = userPreferences.logFilterLevel
        logItems.predicate = { Logger.shouldLog(it.level) }

        userPreferences.logFilterLevelProperty.onChange { level ->
            globalLogLevel = level!!
            logItems.refilter()
        }
    }

    companion object {
        class Style : Stylesheet() {
            companion object {
                val logView by cssclass()

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
}
package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.settings.GeneralSettings
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
class LogView : GamedexScreen("Log") {
    private val settings: GeneralSettings by di()

    private val logItems = SortedFilteredList(Log.entries)

    override fun ToolBar.constructToolbar() {
        enumComboBox(settings.logFilterLevelProperty)
        togglebutton("Tail") {
            settings.logTailProperty.bindBidirectional(selectedProperty())
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

                    toggleClass(Style.trace, false)
                    toggleClass(Style.debug, false)
                    toggleClass(Style.info, false)
                    toggleClass(Style.warn, false)
                    toggleClass(Style.error, false)

                    if (item == null || empty) {
                        text = null
                        return
                    }

                    text = "${item.timestamp.toString("HH:mm:ss.SSS")} [${item.context}] ${item.message}"

                    when (item.level) {
                        LogLevel.trace -> toggleClass(Style.trace, true)
                        LogLevel.debug -> toggleClass(Style.debug, true)
                        LogLevel.info -> toggleClass(Style.info, true)
                        LogLevel.warn -> toggleClass(Style.warn, true)
                        LogLevel.error -> toggleClass(Style.error, true)
                    }
                }
            }
        }

        logItems.onChange {
            if (settings.logTail) {
                scrollTo(items.size)
            }
        }
    }

    init {
        globalLogLevel = settings.logFilterLevel
        logItems.predicate = { Logger.shouldLog(it.level) }

        settings.logFilterLevelProperty.onChange { level ->
            globalLogLevel = level!!
            logItems.refilter()
        }
    }

    companion object {
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
}
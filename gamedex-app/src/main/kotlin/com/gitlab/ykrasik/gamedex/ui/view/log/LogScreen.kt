package com.gitlab.ykrasik.gamedex.ui.view.log

import ch.qos.logback.classic.Level
import com.gitlab.ykrasik.gamedex.settings.GeneralSettings
import com.gitlab.ykrasik.gamedex.ui.map
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.view.GamedexScreen
import com.gitlab.ykrasik.gamedex.util.Log
import com.gitlab.ykrasik.gamedex.util.LogEntry
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
class LogScreen : GamedexScreen("Log", Theme.Icon.book()) {
    private val settings: GeneralSettings by di()

    private val logItems = SortedFilteredList(Log.entries)
    private var displayLevel = settings.logFilterLevelProperty.map(Level::toLevel)

    override fun ToolBar.constructToolbar() {
        combobox(settings.logFilterLevelProperty, listOf(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR).map { it.levelStr.toLowerCase() })
        togglebutton("Tail") {
            selectedProperty().bindBidirectional(settings.logTailProperty)
        }
    }

    override val root = listview(logItems) {
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

                    when (item.level) {
                        Level.TRACE -> toggleClass(Style.trace, true)
                        Level.DEBUG -> toggleClass(Style.debug, true)
                        Level.INFO -> toggleClass(Style.info, true)
                        Level.WARN -> toggleClass(Style.warn, true)
                        Level.ERROR -> toggleClass(Style.error, true)
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
        logItems.predicate = { entry -> entry.level.isGreaterOrEqual(displayLevel.value) }
        settings.logFilterLevelProperty.onChange { logItems.refilter() }
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
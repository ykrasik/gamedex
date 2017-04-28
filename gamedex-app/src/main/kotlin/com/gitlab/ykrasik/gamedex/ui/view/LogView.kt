package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.preferences.UserPreferences
import com.gitlab.ykrasik.gamedex.ui.enumComboBox
import com.gitlab.ykrasik.gamedex.util.*
import javafx.scene.control.ListCell
import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 28/04/2017
 * Time: 11:14
 */
class LogView : View("Log") {
    private val userPreferences: UserPreferences by di()

    private val logItems = SortedFilteredList(Log.entries)

    override val root = borderpane {
        top {
            hbox {
                paddingAll = 10
                enumComboBox(userPreferences.logFilterLevelProperty)
                togglebutton("Show Timestamp") {
                    userPreferences.logShowTimestampProperty.bindBidirectional(selectedProperty())
                }
                togglebutton("Tail") {
                    userPreferences.logTailProperty.bindBidirectional(selectedProperty())
                }
            }
        }
        center {
            // TODO: TableView?
            listview(logItems) {
                addClass(Style.logView)

                setCellFactory {
                    object : ListCell<LogEntry>() {
                        init {
                            userPreferences.logShowTimestampProperty.onChange {
                                updateItem(this.item, this.isEmpty)
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

                            val timestamp = if (userPreferences.logShowTimestamp) {
                                item.timestamp.toString("HH:mm:ss.SSS") + " "
                            } else {
                                ""
                            }

                            // TODO: Color different context differently?
                            text = "$timestamp[${item.context}] ${item.message}"

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
                    }
                }
            }
        }
    }
}
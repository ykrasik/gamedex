package com.gitlab.ykrasik.gamedex.ui.view.game

import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.map
import com.gitlab.ykrasik.gamedex.ui.popOver
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.refreshButton
import com.gitlab.ykrasik.gamedex.ui.toggle
import javafx.beans.property.Property
import javafx.geometry.HPos
import javafx.scene.control.TextField
import javafx.scene.input.MouseEvent
import javafx.scene.text.FontWeight
import org.controlsfx.control.PopOver
import org.joda.time.PeriodType
import org.joda.time.format.PeriodFormatterBuilder
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 10:57
 */
class GameRefreshMenu : View() {
    private val gameController: GameController by di()
    private val settings: GameSettings by di()

    private var staleDurationTextField: TextField by singleAssign()

    override val root = refreshButton {
        enableWhen { gameController.canRunLongTask }

        val staleDurationFormatter = PeriodFormatterBuilder()
            .appendYears().appendSuffix("y").appendSeparator(" ")
            .appendMonths().appendSuffix("mo").appendSeparator(" ")
            .appendDays().appendSuffix("d").appendSeparator(" ")
            .appendHours().appendSuffix("h").appendSeparator(" ")
            .appendMinutes().appendSuffix("m").appendSeparator(" ")
            .appendSeconds().appendSuffix("s").toFormatter()

        val stalePeriodTextProperty = settings.stalePeriodProperty.map {
            val sb = StringBuffer()
            staleDurationFormatter.printTo(sb, it!!.normalizedStandard(PeriodType.yearMonthDayTime()))
            sb.toString()
        }
        val stalePeriodViewModel = PeriodViewModel(stalePeriodTextProperty)
        stalePeriodViewModel.textProperty.onChange { stalePeriodViewModel.commit() }
        stalePeriodViewModel.validate(decorateErrors = true)

        stalePeriodTextProperty.onChange {
            settings.stalePeriod = staleDurationFormatter.parsePeriod(it)
        }

        val leftPopover = popOver(PopOver.ArrowLocation.RIGHT_TOP, closeOnClick = false) {
            gridpane {
                vgap = 5.0
                row {
                    label("Stale Duration") {
                        setId(Style.staleDurationLabel)
                        gridpaneConstraints { hAlignment = HPos.CENTER }
                    }
                }
                row {
                    staleDurationTextField = textfield(stalePeriodViewModel.textProperty) {
                        isFocusTraversable = false
                        validator {
                            val valid = try {
                                staleDurationFormatter.parsePeriod(it); true
                            } catch (e: Exception) {
                                false
                            }
                            if (!valid) error("Invalid duration! Format: {x}y {x}mo {x}d {x}h {x}m {x}s") else null
                        }
                    }
                }
            }
        }
        val downPopover = popOver {
            addEventFilter(MouseEvent.MOUSE_CLICKED) { leftPopover.hide() }
            disableWhen { staleDurationTextField.focusedProperty().or(stalePeriodViewModel.valid.not()) }
            refreshButton("All Stale Games") {
                addClass(CommonStyle.fillAvailableWidth)
                tooltip("Refresh all games that were last refreshed before the stale duration")
                setOnAction { gameController.refreshAllGames() }
            }
            separator()
            refreshButton("Filtered Stale Games") {
                addClass(CommonStyle.fillAvailableWidth)
                tooltip("Refresh filtered games that were last refreshed before the stale duration")
                setOnAction { setOnAction { gameController.refreshFilteredGames() } }
            }
        }
        setOnAction { downPopover.toggle(this); leftPopover.toggle(this) }
    }

    private class PeriodViewModel(p: Property<String>) : ViewModel() {
        val textProperty = bind { p }
    }

    class Style : Stylesheet() {
        companion object {
            val staleDurationLabel by cssid()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            staleDurationLabel {
                fontSize = 15.px
                fontWeight = FontWeight.BOLD
            }
        }
    }
}
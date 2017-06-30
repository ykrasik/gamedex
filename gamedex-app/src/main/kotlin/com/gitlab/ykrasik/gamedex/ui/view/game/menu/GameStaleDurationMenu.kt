package com.gitlab.ykrasik.gamedex.ui.view.game.menu

import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.map
import com.gitlab.ykrasik.gamedex.ui.theme.header
import javafx.beans.property.Property
import javafx.geometry.HPos
import javafx.scene.control.TextField
import org.joda.time.PeriodType
import org.joda.time.format.PeriodFormatterBuilder
import tornadofx.*

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 13:09
 */
class GameStaleDurationMenu : View() {
    private val settings: GameSettings by di()

    private var textField: TextField by singleAssign()

    private val formatter = PeriodFormatterBuilder()
        .appendYears().appendSuffix("y").appendSeparator(" ")
        .appendMonths().appendSuffix("mo").appendSeparator(" ")
        .appendDays().appendSuffix("d").appendSeparator(" ")
        .appendHours().appendSuffix("h").appendSeparator(" ")
        .appendMinutes().appendSuffix("m").appendSeparator(" ")
        .appendSeconds().appendSuffix("s").toFormatter()

    private val stalePeriodTextProperty = settings.stalePeriodProperty.map {
        val sb = StringBuffer()
        formatter.printTo(sb, it!!.normalizedStandard(PeriodType.yearMonthDayTime()))
        sb.toString()
    }

    private val viewModel = PeriodViewModel(stalePeriodTextProperty).apply {
        textProperty.onChange { this@apply.commit() }
        validate(decorateErrors = true)
    }

    override val root = gridpane {
        vgap = 5.0
        row {
            header("Stale Duration") {
                gridpaneConstraints { hAlignment = HPos.CENTER }
            }
        }
        row {
            textField = textfield(viewModel.textProperty) {
                isFocusTraversable = false
                validator {
                    val valid = try {
                        formatter.parsePeriod(it); true
                    } catch (e: Exception) {
                        false
                    }
                    if (!valid) error("Invalid duration! Format: {x}y {x}mo {x}d {x}h {x}m {x}s") else null
                }
            }
        }
    }

    val isValid = viewModel.valid
    val isFocused = textField.focusedProperty()

    init {
        // TODO: Use a bidirectional binding
        stalePeriodTextProperty.onChange {
            settings.stalePeriod = formatter.parsePeriod(it)
        }
    }

    private class PeriodViewModel(p: Property<String>) : ViewModel() {
        val textProperty = bind { p }
    }
}
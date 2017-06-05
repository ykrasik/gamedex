package com.gitlab.ykrasik.gamedex.ui.view.game

import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.popOver
import com.gitlab.ykrasik.gamedex.ui.theme.refreshButton
import com.gitlab.ykrasik.gamedex.ui.toggle
import com.gitlab.ykrasik.gamedex.ui.widgets.adjustableTextField
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.input.MouseEvent
import org.controlsfx.control.PopOver
import org.joda.time.Period
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 10:57
 */
class GameRefreshMenu : View() {
    private val gameController: GameController by di()
    private val settings: GameSettings by di()

    override val root = refreshButton {
        enableWhen { gameController.canRunLongTask }
        val leftPopover = popOver(PopOver.ArrowLocation.RIGHT_TOP, closeOnClick = false) {
            form {
                val years = SimpleObjectProperty(settings.stalePeriod.years).stalePeriod { it::withYears }
                val months = SimpleObjectProperty(settings.stalePeriod.months).stalePeriod { it::withMonths }
                val weeks = SimpleObjectProperty(settings.stalePeriod.weeks).stalePeriod { it::withWeeks }
                val days = SimpleObjectProperty(settings.stalePeriod.days).stalePeriod { it::withDays }
                val hours = SimpleObjectProperty(settings.stalePeriod.hours).stalePeriod { it::withHours }
                val minutes = SimpleObjectProperty(settings.stalePeriod.minutes).stalePeriod { it::withMinutes }
                val seconds = SimpleObjectProperty(settings.stalePeriod.seconds).stalePeriod { it::withSeconds }
                //TODO: This takes up too much space and attention.
                fieldset("Stale Duration") {
                    addClass(Style.staleDurationField)
                    field("Years") { adjustableTextField(years, "years", min = 0, max = Int.MAX_VALUE) }
                    field("Months") { adjustableTextField(months, "months", min = 0, max = 11) }
                    field("Weeks") { adjustableTextField(weeks, "weeks", min = 0, max = 4) }
                    field("Days") { adjustableTextField(days, "days", min = 0, max = 31) }
                    field("Hours") { adjustableTextField(hours, "hours", min = 0, max = 23) }
                    field("Minutes") { adjustableTextField(minutes, "minutes", min = 0, max = 59) }
                    field("Seconds") { adjustableTextField(seconds, "seconds", min = 0, max = 59) }
                }
            }
        }
        val downPopover = popOver {
            addEventFilter(MouseEvent.MOUSE_CLICKED) { leftPopover.hide() }
            refreshButton("All Stale Games") {
                addClass(Style.refreshButton)
                tooltip("Refresh all games that were last refreshed before the stale duration")
                setOnAction { gameController.refreshAllGames() }
            }
            separator()
            refreshButton("Filtered Stale Games") {
                addClass(Style.refreshButton)
                tooltip("Refresh filtered games that were last refreshed before the stale duration")
                setOnAction { setOnAction { gameController.refreshFilteredGames() } }
            }
        }
        setOnAction { downPopover.toggle(this); leftPopover.toggle(this) }
    }

    private fun ObjectProperty<Int>.stalePeriod(f: (Period) -> (Int) -> Period) = apply {
        this.onChange {
            settings.stalePeriod = f(settings.stalePeriod)(it!!)
        }
    }

    class Style : Stylesheet() {
        companion object {
            val staleDurationField by cssclass()
            val refreshButton by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            staleDurationField {
                maxWidth = 200.px
            }

            refreshButton {
                maxWidth = Double.MAX_VALUE.px
                alignment = Pos.CENTER_LEFT
            }
        }
    }
}
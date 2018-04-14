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

package com.gitlab.ykrasik.gamedex.ui.view.game.menu

import com.gitlab.ykrasik.gamedex.core.api.util.mapBidirectional
import com.gitlab.ykrasik.gamedex.core.game.GameUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.game.GameController
import javafx.beans.property.Property
import javafx.geometry.Pos
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
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
    private val userConfigRepository: UserConfigRepository by di()
    private val gameUserConfig = userConfigRepository[GameUserConfig::class]

    private val formatter = PeriodFormatterBuilder()
        .appendYears().appendSuffix("y").appendSeparator(" ")
        .appendMonths().appendSuffix("mo").appendSeparator(" ")
        .appendDays().appendSuffix("d").appendSeparator(" ")
        .appendHours().appendSuffix("h").appendSeparator(" ")
        .appendMinutes().appendSuffix("m").appendSeparator(" ")
        .appendSeconds().appendSuffix("s").toFormatter()

    private val stalePeriodTextProperty = gameUserConfig.stalePeriodSubject.mapBidirectional({
        val sb = StringBuffer()
        formatter.printTo(sb, normalizedStandard(PeriodType.yearMonthDayTime()))
        sb.toString()
    }, { formatter.parsePeriod(this) }).toPropertyCached()


    override val root = buttonWithPopover("Refresh", graphic = Theme.Icon.refresh(), arrowLocation = PopOver.ArrowLocation.TOP_RIGHT, closeOnClick = false) { popover ->
        val viewModel = PeriodViewModel(stalePeriodTextProperty).apply {
            textProperty.onChange { commit() }
            validate(decorateErrors = true)
        }
        labeled("Stale Duration", listOf(CommonStyle.headerLabel)) {
            textfield(viewModel.textProperty) {
                isFocusTraversable = false
                validator {
                    try {
                        formatter.parsePeriod(it)
                        null
                    } catch (e: Exception) {
                        error("Invalid duration! Format: {x}y {x}mo {x}d {x}h {x}m {x}s")
                    }
                }
            }
        }.apply {
            paddingAll = 5.0
        }
        separator()

        val invalid = viewModel.valid.not()
        refreshButton("All Stale Games") {
            useMaxWidth = true
            alignment = Pos.CENTER_LEFT
            disableWhen(invalid)
            tooltip("Refresh all games that were last refreshed before the stale duration")
            setOnAction {
                popover.hide()
                launch(JavaFx) {
                    gameController.refreshAllGames()
                }
            }
        }
        refreshButton("Filtered Stale Games") {
            useMaxWidth = true
            alignment = Pos.CENTER_LEFT
            disableWhen(invalid)
            tooltip("Refresh filtered games that were last refreshed before the stale duration")
            setOnAction {
                popover.hide()
                launch(JavaFx) {
                    gameController.refreshFilteredGames()
                }
            }
        }
    }.apply {
        enableWhen { gameController.canRunLongTask }
    }

    private class PeriodViewModel(p: Property<String>) : ViewModel() {
        val textProperty = bind { p }
    }
}
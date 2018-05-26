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

package com.gitlab.ykrasik.gamedex.core.game.download

import com.gitlab.ykrasik.gamedex.app.api.game.DownloadStaleDurationView
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.PresenterFactory
import com.gitlab.ykrasik.gamedex.core.game.GameUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import org.joda.time.PeriodType
import org.joda.time.format.PeriodFormatterBuilder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 13:08
 */
@Singleton
class GameDownloadStaleDurationPresenterFactory @Inject constructor(
    userConfigRepository: UserConfigRepository
) : PresenterFactory<DownloadStaleDurationView> {
    private val gameUserConfig = userConfigRepository[GameUserConfig::class]

    private val formatter = PeriodFormatterBuilder()
        .appendYears().appendSuffix("y").appendSeparator(" ")
        .appendMonths().appendSuffix("mo").appendSeparator(" ")
        .appendDays().appendSuffix("d").appendSeparator(" ")
        .appendHours().appendSuffix("h").appendSeparator(" ")
        .appendMinutes().appendSuffix("m").appendSeparator(" ")
        .appendSeconds().appendSuffix("s").toFormatter()

    override fun present(view: DownloadStaleDurationView) = object : Presenter() {
        init {
            gameUserConfig.stalePeriodSubject.subscribe {
                val sb = StringBuffer()
                formatter.printTo(sb, it.normalizedStandard(PeriodType.yearMonthDayTime()))
                view.stalePeriodText = sb.toString()
            }

            view.stalePeriodTextChanges.subscribeOnUi(::onStalePeriodTextChanged)
        }

        private fun onStalePeriodTextChanged(stalePeriodText: String) {
            view.stalePeriodValidationError = try {
                gameUserConfig.stalePeriod = formatter.parsePeriod(stalePeriodText)
                null
            } catch (e: Exception) {
                "Invalid duration! Format: {x}y {x}mo {x}d {x}h {x}m {x}s"
            }
        }
    }
}
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

package com.gitlab.ykrasik.gamedex.core.game.presenter.download

import com.gitlab.ykrasik.gamedex.app.api.game.CreatedBeforePeriodView
import com.gitlab.ykrasik.gamedex.app.api.game.UpdatedAfterPeriodView
import com.gitlab.ykrasik.gamedex.app.api.util.BroadcastReceiveChannel
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.settings.GameSettingsRepository
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import kotlinx.coroutines.channels.ReceiveChannel
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.PeriodFormatterBuilder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 13:08
 */
abstract class AbstractRedownloadAfterPeriodPresenter<V>(private val settingsService: SettingsService) : Presenter<V> {
    private val formatter = PeriodFormatterBuilder()
        .appendYears().appendSuffix("y").appendSeparator(" ")
        .appendMonths().appendSuffix("mo").appendSeparator(" ")
        .appendDays().appendSuffix("d").appendSeparator(" ")
        .appendHours().appendSuffix("h").appendSeparator(" ")
        .appendMinutes().appendSuffix("m").appendSeparator(" ")
        .appendSeconds().appendSuffix("s").toFormatter()

    override fun present(view: V) = object : ViewSession() {
        init {
            periodChannel(settingsService.game).forEach {
                val sb = StringBuffer()
                formatter.printTo(sb, it.normalizedStandard(PeriodType.yearMonthDayTime()))
                setPeriodText(view, sb.toString())
            }

            periodTextChanges(view).forEach { onPeriodTextChanged(it) }
        }

        private fun onPeriodTextChanged(periodText: String) {
            val validationError = try {
                val period = formatter.parsePeriod(periodText)
                savePeriod(settingsService.game, period)
                null
            } catch (e: Exception) {
                "Invalid duration! Format: {x}y {x}mo {x}d {x}h {x}m {x}s"
            }
            setValidationError(view, validationError)
        }
    }

    protected abstract fun periodChannel(repo: GameSettingsRepository): BroadcastReceiveChannel<Period>
    protected abstract fun savePeriod(repo: GameSettingsRepository, period: Period)
    protected abstract fun periodTextChanges(view: V): ReceiveChannel<String>
    protected abstract fun setPeriodText(view: V, text: String)
    protected abstract fun setValidationError(view: V, validationError: String?)
}

@Singleton
class GameRedownloadCreatedBeforePeriodPresenter @Inject constructor(settingsService: SettingsService) :
    AbstractRedownloadAfterPeriodPresenter<CreatedBeforePeriodView>(settingsService) {
    override fun periodChannel(repo: GameSettingsRepository) = repo.redownloadCreatedBeforePeriodChannel
    override fun savePeriod(repo: GameSettingsRepository, period: Period) = repo.modify { copy(redownloadCreatedBeforePeriod = period) }

    override fun periodTextChanges(view: CreatedBeforePeriodView) = view.createdBeforePeriodTextChanges
    override fun setPeriodText(view: CreatedBeforePeriodView, text: String) {
        view.createdBeforePeriodText = text
    }

    override fun setValidationError(view: CreatedBeforePeriodView, validationError: String?) {
        view.createdBeforePeriodValidationError = validationError
    }
}

@Singleton
class GameRedownloadUpdatedAfterPeriodPresenter @Inject constructor(settingsService: SettingsService) :
    AbstractRedownloadAfterPeriodPresenter<UpdatedAfterPeriodView>(settingsService) {
    override fun periodChannel(repo: GameSettingsRepository) = repo.redownloadUpdatedAfterPeriodChannel
    override fun savePeriod(repo: GameSettingsRepository, period: Period) = repo.modify { copy(redownloadUpdatedAfterPeriod = period) }

    override fun periodTextChanges(view: UpdatedAfterPeriodView) = view.updatedAfterPeriodTextChanges
    override fun setPeriodText(view: UpdatedAfterPeriodView, text: String) {
        view.updatedAfterPeriodText = text
    }

    override fun setValidationError(view: UpdatedAfterPeriodView, validationError: String?) {
        view.updatedAfterPeriodValidationError = validationError
    }
}
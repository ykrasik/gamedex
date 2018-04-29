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

package com.gitlab.ykrasik.gamedex.app.api.log

import com.gitlab.ykrasik.gamedex.app.api.Presenter
import com.gitlab.ykrasik.gamedex.app.api.View
import com.gitlab.ykrasik.gamedex.app.api.util.ListObservable
import org.joda.time.DateTime

/**
 * User: ykrasik
 * Date: 25/04/2018
 * Time: 21:00
 */
interface LogView : View<LogView.Event> {
    sealed class Event {
        data class LevelChanged(val level: String) : Event()
        data class LogTailChanged(val logTail: Boolean) : Event()
    }

    var entries: ListObservable<LogEntry>

    var level: String
    var logTail: Boolean
}

interface LogPresenter : Presenter<LogView>

data class LogEntry(val level: String, val timestamp: DateTime, val loggerName: String, val message: String, val throwable: Throwable?)
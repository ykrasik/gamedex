/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.log

import com.gitlab.ykrasik.gamedex.app.api.log.LogEntry
import com.gitlab.ykrasik.gamedex.core.util.ListObservable
import com.gitlab.ykrasik.gamedex.core.util.ListObservableImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * User: ykrasik
 * Date: 13/04/2018
 * Time: 20:47
 */
class LogRepository(private val maxLogEntries: Int) : CoroutineScope {
    override val coroutineContext = dispatcher

    private val actor = actor<LogEntry>(capacity = 512) {
        for (entry in channel) {
            _entries += censorEntry(entry)

            if (entries.size > maxLogEntries) {
                _entries -= _entries.subList(0, entries.size - maxLogEntries)
            }
        }
    }

    private val _entries = ListObservableImpl<LogEntry>()
    val entries: ListObservable<LogEntry> = _entries

    private val blacklist = mutableSetOf<String>()

    operator fun plusAssign(entry: LogEntry) {
        actor.trySendBlocking(entry).getOrThrow()
    }

    fun addBlacklistValue(value: String) {
        if (value.isNotBlank()) {
            launch {
                blacklist += value.trim()
                _entries.setAll(_entries.map(::censorEntry))
            }
        }
    }

    private fun censorEntry(entry: LogEntry): LogEntry {
        val censored = blacklist.fold(entry.message) { acc, blacklistedValue ->
            acc.replace(blacklistedValue, "****")
        }
        return entry.copy(message = censored)
    }

    fun clear() = _entries.clear()

    companion object {
        private val dispatcher = Executors.newSingleThreadScheduledExecutor {
            Thread(it, "LogDispatcher").apply { isDaemon = true }
        }.asCoroutineDispatcher()
    }
}

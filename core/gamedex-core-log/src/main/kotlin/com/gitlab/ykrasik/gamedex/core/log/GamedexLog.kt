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

package com.gitlab.ykrasik.gamedex.core.log

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.core.UnsynchronizedAppenderBase
import com.gitlab.ykrasik.gamedex.app.api.log.LogEntry
import com.gitlab.ykrasik.gamedex.app.api.log.LogLevel
import com.gitlab.ykrasik.gamedex.app.api.util.ListObservable
import com.gitlab.ykrasik.gamedex.app.api.util.ListObservableImpl
import org.joda.time.DateTime
import org.slf4j.LoggerFactory.getLogger

/**
 * User: ykrasik
 * Date: 13/04/2018
 * Time: 20:47
 */
object GamedexLog {
    private val maxLogEntries = 10000
    private val _entries = ListObservableImpl<LogEntry>()
    val entries: ListObservable<LogEntry> = _entries

    operator fun plusAssign(entry: LogEntry) {
        _entries += entry

        if (entries.size > maxLogEntries) {
            _entries -= _entries.subList(0, entries.size - maxLogEntries)
        }
    }
}

class GamedexLogAppender : UnsynchronizedAppenderBase<ILoggingEvent>() {
    override fun append(e: ILoggingEvent) {
        GamedexLog += LogEntry(
            level = LogLevel.valueOf(e.level.toString().toLowerCase().capitalize()),
            timestamp = DateTime(e.timeStamp),
            loggerName = e.loggerName.substringAfterLast('.'),
            message = e.message,
            throwable = (e.throwableProxy as? ThrowableProxy)?.throwable
        )
    }

    companion object {
        fun init() {
            with(getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger) {
                addAppender(GamedexLogAppender().apply { start() })
                level = Level.TRACE
            }
        }
    }
}
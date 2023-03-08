/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.util.capitalize
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import java.util.concurrent.atomic.AtomicInteger

/**
 * User: ykrasik
 * Date: 07/02/2019
 * Time: 08:33
 */
class LogServiceImpl(maxLogEntries: Int = 1000) : LogService {
    private val repo = LogRepository(maxLogEntries)
    override val entries = repo.entries

    private val id = AtomicInteger(0)

    private val gameDexLogAppender = object : UnsynchronizedAppenderBase<ILoggingEvent>() {
        init {
            start()
        }

        override fun append(e: ILoggingEvent) {
            repo += LogEntry(
                id = id.incrementAndGet(),
                timestamp = DateTime(e.timeStamp),
                level = LogLevel.valueOf(e.level.toString().lowercase().capitalize()),
                threadName = e.threadName,
                loggerName = e.loggerName.substringAfterLast('.'),
                message = e.formattedMessage,
                throwable = (e.throwableProxy as? ThrowableProxy)?.throwable
            )
        }
    }

    init {
        SLF4JBridgeHandler.removeHandlersForRootLogger()
        SLF4JBridgeHandler.install()
        with(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger) {
            addAppender(gameDexLogAppender)
            level = Level.TRACE
        }
    }

    override fun addBlacklistValue(value: String) = repo.addBlacklistValue(value)

    override fun clear() = repo.clear()
}

package com.gitlab.ykrasik.gamedex.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase
import com.gitlab.ykrasik.gamedex.ui.runLaterIfNecessary
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import tornadofx.observable

/**
 * User: ykrasik
 * Date: 22/04/2017
 * Time: 10:37
 */
object Log {
    private val maxLogEntries = 10000
    val entries = mutableListOf<LogEntry>().observable()

    operator fun plusAssign(entry: LogEntry) = runLaterIfNecessary {
        entries += entry

        if (entries.size > maxLogEntries) {
            entries.remove(0, entries.size - maxLogEntries)
        }
    }
}

data class LogEntry(val level: Level, val timestamp: DateTime, val loggerName: String, val message: String)

class UiLogAppender : UnsynchronizedAppenderBase<ILoggingEvent>() {
    override fun append(e: ILoggingEvent) {
        Log += LogEntry(level = e.level, timestamp = DateTime(e.timeStamp), loggerName = e.loggerName, message = e.message)
    }

    companion object {
        fun init() {
            with(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger) {
                addAppender(UiLogAppender().apply { start() })
                level = Level.TRACE
            }
        }
    }
}
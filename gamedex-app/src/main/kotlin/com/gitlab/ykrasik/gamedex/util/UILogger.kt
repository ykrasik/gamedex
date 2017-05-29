package com.gitlab.ykrasik.gamedex.util

import com.gitlab.ykrasik.gamedex.ui.runLaterIfNecessary
import javafx.collections.FXCollections
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

/**
 * User: ykrasik
 * Date: 22/04/2017
 * Time: 10:37
 */
val UILoggerFactory = { context: String -> UILogger(context) }

class UILogger(private val context: String) : Logger {
    private val stdoutLogger = StdOutLogger(context)

    override fun log(msg: String, level: LogLevel) {
        stdoutLogger.log(msg, level)
        Log += LogEntry(level, context, msg)
    }
}

object Log {
    private val maxLogEntries = 10000
    val entries = FXCollections.observableArrayList<LogEntry>()

    operator fun plusAssign(entry: LogEntry) = runLaterIfNecessary {
        entries += entry

        if (entries.size > maxLogEntries) {
            entries.remove(0, entries.size - maxLogEntries)
        }
    }
}

data class LogEntry(val level: LogLevel, val context: String, val message: String) {
    val timestamp: DateTime = now.withZone(DateTimeZone.getDefault())
}
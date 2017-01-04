package com.gitlab.ykrasik.gamedex.core.ui

import com.github.ykrasik.gamedex.common.KLogger
import javafx.concurrent.Task

/**
 * User: ykrasik
 * Date: 04/01/2017
 * Time: 21:00
 */
abstract class GamedexTask<T>(log: KLogger? = null) : Task<T>() {
    init {
        setOnFailed {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), exception)
        }
        if (log != null) {
            messageProperty().addListener { observableValue, oldValue, newValue ->
                log.info { newValue }
            }
        }
    }

    protected val isStopped: Boolean get() = this.isCancelled || Thread.interrupted()
}
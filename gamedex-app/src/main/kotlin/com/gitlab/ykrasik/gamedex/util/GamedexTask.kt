package com.gitlab.ykrasik.gamedex.util

import com.github.ykrasik.gamedex.common.util.KLogger
import javafx.concurrent.Task
import javafx.concurrent.Worker

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
    protected val isCompleted: Boolean get() = state == Worker.State.SUCCEEDED

    // Composable, may be called multiple times.
    fun onSucceeded(f: (T) -> Unit) {
        val current = onSucceeded
        setOnSucceeded { e ->
            current?.handle(e)
            f(value)
        }
    }
}
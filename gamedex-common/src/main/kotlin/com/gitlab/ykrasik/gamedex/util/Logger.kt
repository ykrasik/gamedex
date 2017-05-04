package com.gitlab.ykrasik.gamedex.util

import org.joda.time.DateTime
import kotlin.reflect.full.companionObject

/**
 * User: ykrasik
 * Date: 28/04/2017
 * Time: 09:09
 */
var globalLogLevel = LogLevel.debug
var LoggerFactory: (String) -> Logger = { context -> StdOutLogger(context) }

interface Logger {
    fun trace(msg: String) = log(msg, LogLevel.trace)
    fun debug(msg: String) = log(msg, LogLevel.debug)
    fun info(msg: String) = log(msg, LogLevel.info)
    fun warn(msg: String) = log(msg, LogLevel.warn)
    fun error(msg: String) = log(msg, LogLevel.error)

    fun log(msg: String, level: LogLevel)

    companion object {
        operator fun invoke(context: String): Logger = LoggerFactory(context)

        fun shouldLog(level: LogLevel): Boolean = level.ordinal >= globalLogLevel.ordinal
    }
}

fun <R : Any> R.logger() = logger(unwrapCompanionClass(this::class.java).simpleName ?: "AnonymousClass")
fun logger(name: String) = LoggerFactory(name)

enum class LogLevel { trace, debug, info, warn, error }

class StdOutLogger(private val context: String) : Logger {
    override fun log(msg: String, level: LogLevel) {
        if (!Logger.shouldLog(level)) return

        val timestamp = DateTime.now().toString("HH:mm:ss.SSS")
        println("$timestamp [${Thread.currentThread().name}] [$level] [$context] $msg")
    }
}

// unwrap companion class to enclosing class given a Java Class
private fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return if (ofClass.enclosingClass != null && ofClass.enclosingClass.kotlin.companionObject?.java == ofClass) {
        ofClass.enclosingClass
    } else {
        ofClass
    }
}
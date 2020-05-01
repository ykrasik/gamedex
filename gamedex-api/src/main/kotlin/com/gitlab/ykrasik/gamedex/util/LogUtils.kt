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

package com.gitlab.ykrasik.gamedex.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import kotlin.reflect.full.companionObject
import kotlin.time.measureTimedValue

/**
 * User: ykrasik
 * Date: 28/04/2017
 * Time: 09:09
 */
fun <R : Any> R.logger(): Logger = logger(unwrapCompanionClass(this::class.java).simpleName ?: "AnonymousClass")

fun logger(name: String): Logger = LoggerFactory.getLogger(if (name.endsWith("Impl")) name.dropLast(4) else name)

// unwrap companion class to enclosing class given a Java Class
private fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> =
    if (ofClass.enclosingClass != null && ofClass.enclosingClass.kotlin.companionObject?.java == ofClass) {
        ofClass.enclosingClass
    } else {
        ofClass
    }

inline fun Logger.trace(crossinline msg: () -> String) {
    if (isTraceEnabled) trace(msg())
}

inline fun Logger.trace(marker: Marker, crossinline msg: (Marker) -> String) {
    if (isTraceEnabled(marker)) trace(marker, msg(marker))
}

inline fun Logger.trace(t: Throwable, crossinline msg: () -> String) {
    if (isTraceEnabled) trace(msg(), t)
}

inline fun Logger.trace(marker: Marker, t: Throwable, crossinline msg: (Marker) -> String) {
    if (isTraceEnabled) trace(marker, msg(marker), t)
}

inline fun Logger.debug(crossinline msg: () -> String) {
    if (isDebugEnabled) debug(msg())
}

inline fun Logger.debug(marker: Marker, crossinline msg: (Marker) -> String) {
    if (isDebugEnabled(marker)) debug(marker, msg(marker))
}

inline fun Logger.debug(t: Throwable, crossinline msg: () -> String) {
    if (isDebugEnabled) debug(msg(), t)
}

inline fun Logger.debug(marker: Marker, t: Throwable, crossinline msg: (Marker) -> String) {
    if (isDebugEnabled(marker)) debug(marker, msg(marker), t)
}

inline fun Logger.info(crossinline msg: () -> String) {
    if (isInfoEnabled) info(msg())
}

inline fun Logger.info(marker: Marker, crossinline msg: (Marker) -> String) {
    if (isInfoEnabled(marker)) info(marker, msg(marker))
}

inline fun Logger.info(t: Throwable, crossinline msg: () -> String) {
    if (isInfoEnabled) info(msg(), t)
}

inline fun Logger.info(marker: Marker, t: Throwable, crossinline msg: (Marker) -> String) {
    if (isInfoEnabled(marker)) info(marker, msg(marker), t)
}

inline fun Logger.warn(crossinline msg: () -> String) {
    if (isWarnEnabled) warn(msg())
}

inline fun Logger.warn(marker: Marker, crossinline msg: (Marker) -> String) {
    if (isWarnEnabled(marker)) warn(marker, msg(marker))
}

inline fun Logger.warn(t: Throwable, crossinline msg: () -> String) {
    if (isWarnEnabled) warn(msg(), t)
}

inline fun Logger.warn(marker: Marker, t: Throwable, crossinline msg: (Marker) -> String) {
    if (isWarnEnabled(marker)) warn(marker, msg(marker), t)
}

inline fun Logger.error(crossinline msg: () -> String) {
    if (isErrorEnabled) error(msg())
}

inline fun Logger.error(marker: Marker, crossinline msg: (Marker) -> String) {
    if (isErrorEnabled(marker)) error(marker, msg(marker))
}

inline fun Logger.error(t: Throwable, crossinline msg: () -> String) {
    if (isErrorEnabled) error(msg(), t)
}

inline fun Logger.error(marker: Marker, t: Throwable, crossinline msg: (Marker) -> String) {
    if (isErrorEnabled(marker)) error(marker, msg(marker), t)
}

inline fun <T> Logger.logResult(
    beforeMessage: String,
    afterMessage: (T) -> String = { "Done: ${it.toString()}" },
    log: Logger.(String) -> Unit = Logger::info,
    f: () -> T
): T {
    log(beforeMessage)
    val result = f()
    log("$beforeMessage ${afterMessage(result)}")
    return result
}

inline fun <T> Logger.time(
    beforeMessage: String,
    afterMessage: (String, T) -> String = { time, _ -> time },
    log: Logger.(String) -> Unit = Logger::info,
    f: () -> T
): T {
    log(beforeMessage)
    val (result, timeTaken) = measureTimedValue(f)
    log("$beforeMessage Done: ${afterMessage(timeTaken.humanReadable, result)}")
    return result
}
package com.gitlab.ykrasik.gamedex.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import kotlin.reflect.full.companionObject

/**
 * User: ykrasik
 * Date: 28/04/2017
 * Time: 09:09
 */
fun <R : Any> R.logger() = logger(unwrapCompanionClass(this::class.java).simpleName ?: "AnonymousClass")

fun logger(name: String) = LoggerFactory.getLogger(if (name.endsWith("Impl")) name.dropLast(4) else name)

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
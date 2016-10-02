package com.github.ykrasik.gamedex.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.companionObject

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 10:02
 */

fun <R : Any> R.logger(): Lazy<Logger> = lazy {
    LoggerFactory.getLogger(unwrapCompanionClass(this.javaClass).name)
}

// unwrap companion class to enclosing class given a Java Class
private fun <T: Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return if (ofClass.enclosingClass != null && ofClass.enclosingClass.kotlin.companionObject?.java == ofClass) {
        ofClass.enclosingClass
    } else {
        ofClass
    }
}

inline fun Logger.e(msg: () -> String) = error(msg)
inline fun Logger.error(msg: () -> String) {
    if (isErrorEnabled) error(msg())
}

inline fun Logger.w(msg: () -> String) = warn(msg)
inline fun Logger.warn(msg: () -> String) {
    if (isWarnEnabled) warn(msg())
}

inline fun Logger.i(msg: () -> String) = info(msg)
inline fun Logger.info(msg: () -> String) {
    if (isInfoEnabled) info(msg())
}

inline fun Logger.d(msg: () -> String) = debug(msg)
inline fun Logger.debug(msg: () -> String) {
    if (isDebugEnabled) debug(msg())
}

inline fun Logger.t(msg: () -> String) = trace(msg)
inline fun Logger.trace(msg: () -> String) {
    if (isTraceEnabled) trace(msg())
}
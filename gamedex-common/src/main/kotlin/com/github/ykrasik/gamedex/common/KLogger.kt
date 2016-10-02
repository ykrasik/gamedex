package com.github.ykrasik.gamedex.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.companionObject

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 10:02
 */
class KLogger(clazz: Class<Any>) {
    val logger: Logger = LoggerFactory.getLogger(unwrapCompanionClass(clazz).name)

    inline fun error(msg: () -> String): Unit { if (logger.isErrorEnabled) logger.error(msg()) }
    inline fun warn(msg: () -> String): Unit { if (logger.isWarnEnabled) logger.warn(msg()) }
    inline fun info(msg: () -> String): Unit { if (logger.isInfoEnabled) logger.info(msg()) }
    inline fun debug(msg: () -> String): Unit { if (logger.isDebugEnabled) logger.debug(msg()) }
    inline fun trace(msg: () -> String): Unit { if (logger.isTraceEnabled) logger.trace(msg()) }

    // unwrap companion class to enclosing class given a Java Class
    private fun <T: Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
        return if (ofClass.enclosingClass != null && ofClass.enclosingClass.kotlin.companionObject?.java == ofClass) {
            ofClass.enclosingClass
        } else {
            ofClass
        }
    }
}

fun <R : Any> R.logger(): Lazy<KLogger> = lazy {
    KLogger(this.javaClass)
}
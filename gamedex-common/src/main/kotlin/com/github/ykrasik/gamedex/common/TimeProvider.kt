package com.github.ykrasik.gamedex.common

import org.joda.time.DateTime

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 12:02
 */
open class TimeProvider {
    open fun now(): DateTime = DateTime.now()
}
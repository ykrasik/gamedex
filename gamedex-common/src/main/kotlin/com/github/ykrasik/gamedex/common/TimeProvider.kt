package com.github.ykrasik.gamedex.common

import org.joda.time.DateTime

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 12:02
 */
interface TimeProvider {
    fun now(): DateTime
}

class DefaultTimeProvider : TimeProvider {
    override fun now(): DateTime = DateTime.now()
}
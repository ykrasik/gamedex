package com.github.ykrasik.gamedex.common.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * User: ykrasik
 * Date: 26/05/2016
 * Time: 15:41
 */

fun Date.toLocalDate(timeZoneId: ZoneId = ZoneId.systemDefault()): LocalDate = toLocalDateTime(timeZoneId).toLocalDate()

fun Date.toLocalDateTime(timeZoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
    return toInstant().atZone(timeZoneId).toLocalDateTime()
}

fun LocalDate.toDate(timeZoneId: ZoneId = ZoneId.systemDefault()): Date = atStartOfDay().toDate(timeZoneId)

fun LocalDateTime.toDate(timeZoneId: ZoneId = ZoneId.systemDefault()): Date {
    return Date.from(atZone(timeZoneId).toInstant())
}
package com.gitlab.ykrasik.gamedex.util

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 28/05/2017
 * Time: 22:05
 */
val now: DateTime get() = DateTime.now(DateTimeZone.UTC)
val today: LocalDate get() = LocalDate.now(DateTimeZone.UTC)

fun Long.toDateTime(): DateTime = DateTime(this, DateTimeZone.UTC)
fun String.toDate(): LocalDate = LocalDate.parse(this)
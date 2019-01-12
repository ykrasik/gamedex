/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.Period
import org.joda.time.format.PeriodFormat

/**
 * User: ykrasik
 * Date: 28/05/2017
 * Time: 22:05
 */

typealias JodaLocalDate = org.joda.time.LocalDate
typealias JodaDateTime = org.joda.time.DateTime
typealias JavaLocalDate = java.time.LocalDate
typealias JavaDateTime = java.time.LocalDateTime

val now: JodaDateTime get() = DateTime.now(DateTimeZone.UTC)
val today: JodaLocalDate get() = LocalDate.now(DateTimeZone.UTC)

fun Long.toDateTime(): JodaDateTime = DateTime(this, DateTimeZone.UTC)

fun String.toDate(): JodaLocalDate = LocalDate.parse(this)
fun String.toDateOrNull(): JodaLocalDate? = runCatching { toDate() }.getOrNull()

fun String.toDateTime(): JodaDateTime = DateTime.parse(this)
fun String.toDateTimeOrNull(): JodaDateTime? = runCatching { toDateTime() }.getOrNull()

val JodaLocalDate.java get(): JavaLocalDate = JavaLocalDate.of(year, monthOfYear, dayOfMonth)
val JavaLocalDate.joda get(): JodaLocalDate = LocalDate(year, monthValue, dayOfMonth)

fun JodaDateTime.toHumanReadable(): String = toString("yyyy-MM-dd HH:mm:ss")
fun Period.toHumanReadable(): String = PeriodFormat.getDefault().print(this)
fun Long.toHumanReadableDuration(): String = Period(this).toHumanReadable()

val Int.years get() = Period.years(this)
val Int.months get() = Period.months(this)
val Int.weeks get() = Period.weeks(this)
val Int.days get() = Period.days(this)
val Int.hours get() = Period.hours(this)
val Int.minutes get() = Period.minutes(this)
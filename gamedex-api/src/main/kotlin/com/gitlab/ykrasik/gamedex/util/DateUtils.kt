/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

val Long.dateTime: JodaDateTime get() = DateTime(this, DateTimeZone.UTC)

val String.date: JodaLocalDate get() = LocalDate.parse(this)
val String.dateOrNull: JodaLocalDate? get() = runCatching { date }.getOrNull()

val String.dateTime: JodaDateTime get() = DateTime.parse(this)
val String.dateTimeOrNull: JodaDateTime? get() = runCatching { dateTime }.getOrNull()

val JodaLocalDate.java get(): JavaLocalDate = JavaLocalDate.of(year, monthOfYear, dayOfMonth)
val JavaLocalDate.joda get(): JodaLocalDate = LocalDate(year, monthValue, dayOfMonth)

val JodaDateTime.defaultTimeZone: JodaDateTime get() = withZone(DateTimeZone.getDefault())
val JodaDateTime.humanReadable: String get() = toString("yyyy-MM-dd HH:mm")
val Period.humanReadable: String get() = PeriodFormat.getDefault().print(this)
val kotlin.time.Duration.humanReadable: String get() = toString()

val Int.years get() = Period.years(this)
val Int.months get() = Period.months(this)
val Int.weeks get() = Period.weeks(this)
val Int.days get() = Period.days(this)
val Int.hours get() = Period.hours(this)
val Int.minutes get() = Period.minutes(this)
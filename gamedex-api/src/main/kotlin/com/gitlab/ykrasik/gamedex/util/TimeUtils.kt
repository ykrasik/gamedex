/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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

import java.util.concurrent.TimeUnit

/**
 * User: ykrasik
 * Date: 04/06/2018
 * Time: 09:44
 */
private val timeUnits = listOf(
    TimeUnit.DAYS to "d",
    TimeUnit.HOURS to "h",
    TimeUnit.MINUTES to "m",
    TimeUnit.SECONDS to "s",
    TimeUnit.MILLISECONDS to "ms"
)

fun Long.toHumanReadableDuration(): String {
    var accumulated = this

    val builder = StringBuilder()
    timeUnits.forEach { (timeUnit, name) ->
        val convertedDuration = timeUnit.convert(accumulated, TimeUnit.MILLISECONDS)
        if (convertedDuration > 0) {
            builder.append(convertedDuration).append(name).append('.')
            accumulated -= TimeUnit.MILLISECONDS.convert(convertedDuration, timeUnit)
        }
    }
    return if (builder.isEmpty()) "0ms" else builder.substring(0, builder.length - 1)
}
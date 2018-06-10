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

package com.gitlab.ykrasik.gamedex.core.settings

import com.gitlab.ykrasik.gamedex.app.api.log.LogLevel
import java.io.File

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:10
 */
class GeneralSettingsRepository : SettingsRepository<GeneralSettingsRepository.Data>("general", Data::class) {
    data class Data(
        val prevDirectory: File,
        val exportDbDirectory: File,
        val logFilterLevel: LogLevel,
        val logTail: Boolean
    )

    override fun defaultSettings() = Data(
        prevDirectory = File("."),
        exportDbDirectory = File("."),
        logFilterLevel = LogLevel.Info,
        logTail = true
    )

    val prevDirectoryChannel = channel(Data::prevDirectory)
    val prevDirectory by prevDirectoryChannel

    val exportDbDirectoryChannel = channel(Data::exportDbDirectory)
    val exportDbDirectory by exportDbDirectoryChannel

    val logFilterLevelChannel = channel(Data::logFilterLevel)
    val logFilterLevel by logFilterLevelChannel

    val logTailChannel = channel(Data::logTail)
    val logTail by logTailChannel
}
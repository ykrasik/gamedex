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

package com.gitlab.ykrasik.gamedex.core.settings

import com.gitlab.ykrasik.gamedex.app.api.log.LogLevel
import com.gitlab.ykrasik.gamedex.app.api.util.MultiReceiveChannel
import com.gitlab.ykrasik.gamedex.core.util.modify
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:10
 */
@Singleton
class GeneralSettingsRepository @Inject constructor(repo: SettingsRepository) {
    data class Data(
        val prevDirectory: File,
        val exportDbDirectory: File,
        val logFilterLevel: LogLevel,
        val logTail: Boolean,
        val useInternalBrowser: Boolean,
        val searchResultLimit: Int
    )

    private val storage = repo.storage(basePath = "", name = "general") {
        Data(
            prevDirectory = File("."),
            exportDbDirectory = File("."),
            logFilterLevel = LogLevel.Info,
            logTail = true,
            useInternalBrowser = true,
            searchResultLimit = 10
        )
    }

    val dataChannel: MultiReceiveChannel<Data> = storage.valueChannel
    fun modify(f: Data.() -> Data) = storage.modify(f)

    val prevDirectoryChannel = storage.biChannel(Data::prevDirectory) { copy(prevDirectory = it) }
    var prevDirectory by prevDirectoryChannel

    val exportDbDirectoryChannel = storage.biChannel(Data::exportDbDirectory) { copy(exportDbDirectory = it) }
    var exportDbDirectory by exportDbDirectoryChannel

    val logFilterLevelChannel = storage.biChannel(Data::logFilterLevel) { copy(logFilterLevel = it) }
    var logFilterLevel by logFilterLevelChannel

    val logTailChannel = storage.biChannel(Data::logTail) { copy(logTail = it) }
    var logTail by logTailChannel

    val useInternalBrowserChannel = storage.biChannel(Data::useInternalBrowser) { copy(useInternalBrowser = it) }
    var useInternalBrowser by useInternalBrowserChannel

    val searchResultLimitChannel = storage.biChannel(Data::searchResultLimit) { copy(searchResultLimit = it) }
    var searchResultLimit by searchResultLimitChannel
}
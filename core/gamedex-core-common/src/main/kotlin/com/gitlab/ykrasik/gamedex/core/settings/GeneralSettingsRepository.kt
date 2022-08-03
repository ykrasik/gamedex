/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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
        val useInternalBrowser: Boolean,
        val customBrowserCommand: String,
        val searchResultLimit: Int,
    )

    private val storage = repo.storage(basePath = "", name = "general") {
        Data(
            prevDirectory = File("."),
            exportDbDirectory = File("."),
            logFilterLevel = LogLevel.Info,
            useInternalBrowser = true,
            customBrowserCommand = "",
            searchResultLimit = 10
        )
    }

    val prevDirectory = storage.biMap(Data::prevDirectory) { copy(prevDirectory = it) }
    val exportDbDirectory = storage.biMap(Data::exportDbDirectory) { copy(exportDbDirectory = it) }
    val logFilterLevel = storage.biMap(Data::logFilterLevel) { copy(logFilterLevel = it) }
    val useInternalBrowser = storage.biMap(Data::useInternalBrowser) { copy(useInternalBrowser = it) }
    val customBrowserCommand = storage.biMap(Data::customBrowserCommand) { copy(customBrowserCommand = it) }
    val searchResultLimit = storage.biMap(Data::searchResultLimit) { copy(searchResultLimit = it) }
}

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

package com.gitlab.ykrasik.gamedex.core.general

import com.gitlab.ykrasik.gamedex.core.settings.SettingsRepo
import com.gitlab.ykrasik.gamedex.core.settings.UserSettings
import java.io.File
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:10
 */
@Singleton
class GeneralSettings : UserSettings() {
    override val repo = SettingsRepo("general") {
        Data(
            prevDirectory = File("."),
            exportDbDirectory = File("."),
            logFilterLevel = "Info",
            logTail = true
        )
    }

    val prevDirectorySubject = repo.subject(Data::prevDirectory) { copy(prevDirectory = it) }
    var prevDirectory by prevDirectorySubject

    val exportDbDirectorySubject = repo.subject(Data::exportDbDirectory) { copy(exportDbDirectory = it) }
    var exportDbDirectory by exportDbDirectorySubject

    val logFilterLevelSubject = repo.subject(Data::logFilterLevel) { copy(logFilterLevel = it) }
    var logFilterLevel by logFilterLevelSubject

    val logTailSubject = repo.subject(Data::logTail) { copy(logTail = it) }
    var logTail by logTailSubject

    data class Data(
        val prevDirectory: File,
        val exportDbDirectory: File,
        val logFilterLevel: String,
        val logTail: Boolean
    )
}
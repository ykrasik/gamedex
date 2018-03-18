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

package com.gitlab.ykrasik.gamedex.settings

import ch.qos.logback.classic.Level
import tornadofx.getValue
import tornadofx.setValue
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
            prevDirectory = null,
            exportDbDirectory = null,
            logFilterLevel = Level.INFO.levelStr.toLowerCase(),
            logTail = true
        )
    }

    val prevDirectoryProperty = repo.property(Data::prevDirectory) { copy(prevDirectory = it) }
    var prevDirectory by prevDirectoryProperty

    val exportDbDirectoryProperty = repo.property(Data::exportDbDirectory) { copy(exportDbDirectory = it) }
    var exportDbDirectory by exportDbDirectoryProperty

    val logFilterLevelProperty = repo.stringProperty(Data::logFilterLevel) { copy(logFilterLevel = it) }
    var logFilterLevel by logFilterLevelProperty

    val logTailProperty = repo.booleanProperty(Data::logTail) { copy(logTail = it) }
    var logTail by logTailProperty

    data class Data(
        val prevDirectory: File?,
        val exportDbDirectory: File?,
        val logFilterLevel: String,
        val logTail: Boolean
    )
}
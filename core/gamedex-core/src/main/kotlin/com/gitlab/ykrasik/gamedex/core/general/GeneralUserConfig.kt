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

import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigScope
import java.io.File
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:10
 */
@Singleton
class GeneralUserConfig : UserConfig() {
    override val scope = UserConfigScope("general") {
        Data(
            prevDirectory = File("."),
            exportDbDirectory = File("."),
            logFilterLevel = "Info",
            logTail = true
        )
    }

    val prevDirectorySubject = scope.subject(Data::prevDirectory) { copy(prevDirectory = it) }
    var prevDirectory by prevDirectorySubject

    val exportDbDirectorySubject = scope.subject(Data::exportDbDirectory) { copy(exportDbDirectory = it) }
    var exportDbDirectory by exportDbDirectorySubject

    val logFilterLevelSubject = scope.subject(Data::logFilterLevel) { copy(logFilterLevel = it) }
    var logFilterLevel by logFilterLevelSubject

    val logTailSubject = scope.subject(Data::logTail) { copy(logTail = it) }
    var logTail by logTailSubject

    data class Data(
        val prevDirectory: File,
        val exportDbDirectory: File,
        val logFilterLevel: String,
        val logTail: Boolean
    )
}
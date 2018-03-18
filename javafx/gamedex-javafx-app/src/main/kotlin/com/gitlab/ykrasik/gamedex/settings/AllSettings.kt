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

import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 11/03/2018
 * Time: 15:12
 */
@Singleton
class AllSettings @Inject constructor(
    val game: GameSettings,
    val gameWall: GameWallSettings,
    val general: GeneralSettings,
    val preloader: PreloaderSettings,
    val provider: ProviderSettings,
    val report: ReportSettings
) {
    private val all = listOf(game, gameWall, general, preloader, provider, report)

    fun saveSnapshot() = all.forEach {
        it.disableWrite()
        it.saveSnapshot()
    }

    fun restoreSnapshot() = all.forEach {
        it.restoreSnapshot()
        it.enableWrite()
        it.clearSnapshot()
    }

    fun commitSnapshot() = all.forEach {
        it.enableWrite()
        it.flush()
        it.clearSnapshot()
    }
}
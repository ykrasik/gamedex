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

import com.gitlab.ykrasik.gamedex.core.settings.SettingsRepo
import com.gitlab.ykrasik.gamedex.core.settings.UserSettings
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 09/03/2018
 * Time: 09:28
 */
@Singleton
class PreloaderSettings : UserSettings() {
    override val repo = SettingsRepo("preloader") {
        Data(
            diComponents = 24
        )
    }

    val diComponentsSubject = repo.subject(Data::diComponents) { copy(diComponents = it) }
    var diComponents by diComponentsSubject

    data class Data(
        val diComponents: Int
    )
}
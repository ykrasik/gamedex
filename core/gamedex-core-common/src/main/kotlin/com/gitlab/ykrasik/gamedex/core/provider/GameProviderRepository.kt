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

package com.gitlab.ykrasik.gamedex.core.provider

import com.gitlab.ykrasik.gamedex.core.plugin.PluginManager
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 07/04/2017
 * Time: 21:30
 */
// FIXME: Hide this.
@Singleton
class GameProviderRepository @Inject constructor(pluginManager: PluginManager) {
    val allProviders: List<GameProvider> = pluginManager.getImplementations(GameProvider::class).sortedBy { it.id }
}
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

package com.gitlab.ykrasik.gamedex.provider.giantbomb.module

import com.gitlab.ykrasik.gamedex.provider.ProviderModule
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombConfig
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombProvider
import com.google.inject.Provides
import com.typesafe.config.Config
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:56
 */
@Suppress("unused")
object GiantBombModule : ProviderModule() {
    override fun configure() {
        bindProvider<GiantBombProvider>()
    }

    @Provides
    @Singleton
    fun giantBombConfig(config: Config) = GiantBombConfig(config)
}
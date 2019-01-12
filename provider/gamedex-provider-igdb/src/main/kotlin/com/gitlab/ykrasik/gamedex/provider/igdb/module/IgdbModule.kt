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

package com.gitlab.ykrasik.gamedex.provider.igdb.module

import com.gitlab.ykrasik.gamedex.provider.ProviderModule
import com.gitlab.ykrasik.gamedex.provider.igdb.IgdbConfig
import com.gitlab.ykrasik.gamedex.provider.igdb.IgdbProvider
import com.google.inject.Provides
import com.typesafe.config.Config
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 05/02/2017
 * Time: 21:51
 */
@Suppress("unused")
object IgdbModule : ProviderModule() {
    override fun configure() {
        bindProvider<IgdbProvider>()
    }

    @Provides
    @Singleton
    fun igdbConfig(config: Config) = IgdbConfig(config)
}

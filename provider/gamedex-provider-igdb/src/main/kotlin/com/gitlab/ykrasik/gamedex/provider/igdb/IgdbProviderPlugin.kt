/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.provider.igdb

import com.gitlab.ykrasik.gamedex.plugin.DefaultPlugin
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderStorageFactory
import com.gitlab.ykrasik.gamedex.provider.create
import com.gitlab.ykrasik.gamedex.util.SingleValueStorage
import com.google.inject.Provides
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 05/02/2017
 * Time: 21:51
 */
@Suppress("unused")
object IgdbProviderPlugin : DefaultPlugin() {
    override val descriptor = readPluginDescriptor("/com/gitlab/ykrasik/gamedex/provider/igdb/plugin.json")

    override fun configure() {
        bind(GameProvider::class.java).to(IgdbProvider::class.java)
    }

    @Provides
    @Singleton
    fun igdbConfig(config: Config) =
        IgdbConfig(
            config.withFallback(
                ConfigFactory.load(javaClass.classLoader, "com/gitlab/ykrasik/gamedex/provider/igdb/igdb.conf")
            )
        )

    @Provides
    @Singleton
    @IgdbStorage
    fun igdbStorage(factory: ProviderStorageFactory): SingleValueStorage<IgdbStorageData> = factory.create("igdb")
}
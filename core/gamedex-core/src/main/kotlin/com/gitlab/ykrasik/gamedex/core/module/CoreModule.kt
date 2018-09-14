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

package com.gitlab.ykrasik.gamedex.core.module

import com.gitlab.ykrasik.gamedex.FileStructure
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.ViewRegistry
import com.gitlab.ykrasik.gamedex.core.ViewRegistryImpl
import com.gitlab.ykrasik.gamedex.core.api.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.api.image.ImageRepository
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.cache.Cache
import com.gitlab.ykrasik.gamedex.core.cache.JsonCacheSerDes
import com.gitlab.ykrasik.gamedex.core.cache.PersistedCache
import com.gitlab.ykrasik.gamedex.core.file.FileSystemServiceImpl
import com.gitlab.ykrasik.gamedex.core.file.NewDirectoryDetector
import com.gitlab.ykrasik.gamedex.core.game.GameConfig
import com.gitlab.ykrasik.gamedex.core.game.GameServiceImpl
import com.gitlab.ykrasik.gamedex.core.image.ImageConfig
import com.gitlab.ykrasik.gamedex.core.image.ImageRepositoryImpl
import com.gitlab.ykrasik.gamedex.core.library.LibraryServiceImpl
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderServiceImpl
import com.gitlab.ykrasik.gamedex.core.report.ReportUserConfig
import com.gitlab.ykrasik.gamedex.core.settings.FileSettingsStorageFactory
import com.gitlab.ykrasik.gamedex.core.settings.SettingsStorageFactory
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import com.gitlab.ykrasik.gamedex.core.util.ClassPathScanner
import com.gitlab.ykrasik.gamedex.provider.ProviderModule
import com.gitlab.ykrasik.gamedex.util.info
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.md5
import com.gitlab.ykrasik.gamedex.util.time
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.multibindings.Multibinder
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 27/03/2018
 * Time: 21:55
 */
object CoreModule : AbstractModule() {
    private val log = logger("Core")

    override fun configure() {
        // Install all providers detected by classpath scan
        log.info { "Detecting providers..." }
        log.time({ "Detecting providers took $it" }) {
            ClassPathScanner.scanSubTypes("com.gitlab.ykrasik.gamedex.provider", ProviderModule::class).forEach {
                install(it.kotlin.objectInstance!!)
            }
        }

        bind(ViewRegistry::class.java).to(ViewRegistryImpl::class.java)

        bind(LibraryService::class.java).to(LibraryServiceImpl::class.java)
        bind(GameService::class.java).to(GameServiceImpl::class.java)
        bind(GameProviderService::class.java).to(GameProviderServiceImpl::class.java)

        bind(FileSystemService::class.java).to(FileSystemServiceImpl::class.java)
        bind(ImageRepository::class.java).to(ImageRepositoryImpl::class.java)

        bind(SettingsStorageFactory::class.java).toInstance(FileSettingsStorageFactory)

        with(Multibinder.newSetBinder(binder(), UserConfig::class.java)) {
            addBinding().to(ReportUserConfig::class.java)
        }
        bind(UserConfigRepository::class.java)
    }

    @Provides
    @Singleton
    fun config(): Config {
        log.info("Detecting configuration...")
        val configurationFiles = log.time({ "Detecting configuration took $it" }) {
            ClassPathScanner.scanResources("com.gitlab.ykrasik.gamedex") {
                it.endsWith(".conf") && it != "application.conf" && it != "reference.conf"
            }
        }

        // Use the default config as a baseline and apply 'withFallback' on it for every custom .conf file encountered.
        log.info("Loading configuration...")
        return log.time({ "Loading configuration took $it" }) {
            configurationFiles.fold(ConfigFactory.load()) { current, url ->
                current.withFallback(ConfigFactory.parseURL(url))
            }
        }
    }

    @Provides
    @Singleton
    fun newDirectoryDetector(config: Config) =
        Class.forName(config.getString("gameDex.newDirectoryDetector.class")).newInstance() as NewDirectoryDetector

    @Provides
    @Singleton
    fun gameConfig(config: Config): GameConfig = config.extract("gameDex.game")

    @Provides
    @Singleton
    fun imageConfig(config: Config): ImageConfig = config.extract("gameDex.image")

    @Provides
    @Singleton
    fun fileStructureCache(): Cache<Game, FileStructure> {
        log.info("Reading file system cache...")
        return log.time({ "Reading file system cache: $it" }) {
            PersistedCache("cache/filesystem", JsonCacheSerDes()) {
                "[${it.path.toString().md5()}] ${it.path.name}.json"
            }
        }
    }
}
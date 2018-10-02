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
import com.gitlab.ykrasik.gamedex.GameId
import com.gitlab.ykrasik.gamedex.app.api.ViewRegistry
import com.gitlab.ykrasik.gamedex.core.ViewRegistryImpl
import com.gitlab.ykrasik.gamedex.core.api.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.file.FileStructureStorage
import com.gitlab.ykrasik.gamedex.core.file.FileSystemServiceImpl
import com.gitlab.ykrasik.gamedex.core.file.NewDirectoryDetector
import com.gitlab.ykrasik.gamedex.core.game.module.GameModule
import com.gitlab.ykrasik.gamedex.core.image.ImageConfig
import com.gitlab.ykrasik.gamedex.core.image.ImageStorage
import com.gitlab.ykrasik.gamedex.core.library.module.LibraryModule
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderServiceImpl
import com.gitlab.ykrasik.gamedex.core.report.module.ReportModule
import com.gitlab.ykrasik.gamedex.core.storage.*
import com.gitlab.ykrasik.gamedex.core.util.ClassPathScanner
import com.gitlab.ykrasik.gamedex.provider.ProviderModule
import com.gitlab.ykrasik.gamedex.util.*
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.TypeLiteral
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
        log.time("Detecting providers...", { time, providers -> "${providers.size} providers in $time" }) {
            val providers = ClassPathScanner.scanSubTypes("com.gitlab.ykrasik.gamedex.provider", ProviderModule::class)
            providers.forEach { install(it.kotlin.objectInstance!!) }
            providers
        }

        install(LibraryModule)
        install(GameModule)
        install(ReportModule)

        bind(ViewRegistry::class.java).to(ViewRegistryImpl::class.java)

        bind(GameProviderService::class.java).to(GameProviderServiceImpl::class.java)

        bind(FileSystemService::class.java).to(FileSystemServiceImpl::class.java)

        bind(object : TypeLiteral<JsonStorageFactory<Int>>() {}).toInstance(IntIdJsonStorageFactory)
        bind(object : TypeLiteral<JsonStorageFactory<String>>() {}).toInstance(StringIdJsonStorageFactory)

    }

    @Provides
    @Singleton
    fun config(): Config = log.time("Loading configuration...") {
        val configurationFiles = ClassPathScanner.scanResources("com.gitlab.ykrasik.gamedex") {
            it.endsWith(".conf") && it != "application.conf" && it != "reference.conf"
        }

        // Use the default config as a baseline and apply 'withFallback' on it for every custom .conf file encountered.
        configurationFiles.fold(ConfigFactory.load()) { current, url ->
            current.withFallback(ConfigFactory.parseURL(url))
        }
    }

    @Provides
    @Singleton
    fun newDirectoryDetector(config: Config) =
        Class.forName(config.getString("gameDex.newDirectoryDetector.class")).newInstance() as NewDirectoryDetector

    @Provides
    @Singleton
    fun imageConfig(config: Config): ImageConfig = config.extract("gameDex.image")

    @Provides
    @Singleton
    @FileStructureStorage
    fun fileStructureStorage(): Storage<GameId, FileStructure> = log.time("Reading file system cache...") {
        FileStorage.json<FileStructure>("cache/file_structure").intId().memoryCached()
    }

    @Provides
    @Singleton
    @ImageStorage
    fun imageStorage(): Storage<String, ByteArray> =
        FileStorage.binary("cache/images").stringId(
            keyTransform = { url -> "${url.base64Encoded()}.${url.toUrl().filePath.extension}" },
            reverseKeyTransform = { fileName -> fileName.substringBeforeLast(".").base64Decoded() }
        )
}
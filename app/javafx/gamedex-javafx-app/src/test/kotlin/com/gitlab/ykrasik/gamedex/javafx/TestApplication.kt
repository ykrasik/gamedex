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

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.common.FakeServerManager
import com.gitlab.ykrasik.gamedex.core.common.using
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceConfig
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceServiceImpl
import com.gitlab.ykrasik.gamedex.core.plugin.ClasspathPluginScanner
import com.gitlab.ykrasik.gamedex.core.plugin.DirectoryPluginScanner
import com.gitlab.ykrasik.gamedex.core.plugin.PluginManagerImpl
import com.gitlab.ykrasik.gamedex.core.provider.ProviderStorageFactoryImpl
import com.gitlab.ykrasik.gamedex.provider.ProviderStorageFactory
import com.gitlab.ykrasik.gamedex.test.*
import com.gitlab.ykrasik.gamedex.util.humanReadable
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Provides
import com.google.inject.Singleton
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.Executors
import kotlin.time.measureTime

/**
 * User: ykrasik
 * Date: 18/03/2017
 * Time: 21:59
 */
object TestApplication {
    val dbUrl = "jdbc:h2:./test"
    val persistenceService by lazy { PersistenceServiceImpl(PersistenceConfig(dbUrl, "org.h2.Driver", "sa", "")) }

    @JvmStatic
    fun main(args: Array<String>) {
        System.setProperty("kotlinx.coroutines.debug", "on")

        System.setProperty("gameDex.env", "dev")
        System.setProperty("gameDex.provider.skipCredentialValidation", "true")
        System.setProperty("gameDex.persistence.dbUrl", dbUrl)

        // Pre-Load test images
        randomImage()

        val pluginManager = PluginManagerImpl(
            Guice.createInjector(TestModule),
            listOf(DirectoryPluginScanner("plugin-testkits"), ClasspathPluginScanner())
        )

        val fakeServerManager = FakeServerManager(pluginManager.getImplementations(FakeServerFactory::class))
        fakeServerManager.using {
            if (System.getProperty("gameDex.persistence.noGenerateDb") == null) {
                generateDb(
                    numGames = 5000
//                    numGames = null
                )
            }

            Main.main(args)
        }
    }

    private fun FakeServerManager.generateDb(numGames: Int?) {
        persistenceService.dropDb()
        if (numGames == null) return

        val timeTaken = measureTime {
            val basePath = File(".").absoluteFile.normalize()
            val libraries = listOf(
                Triple(LibraryType.Digital, Platform.Windows, "app"),
                Triple(LibraryType.Digital, Platform.Android, "build"),
                Triple(LibraryType.Digital, Platform.Mac, "data"),
                Triple(LibraryType.Excluded, null, "core"),
                Triple(LibraryType.Digital, Platform.Windows, "provider")
            ).map { (type, platform, name) ->
                persistenceService.insertLibrary(LibraryData(name, basePath.resolve(name), type, platform))
            }.filter { it.type != LibraryType.Excluded }

            Executors.newFixedThreadPool(10).asCoroutineDispatcher().use { context ->
                runBlocking {
                    (0 until numGames).map {
                        async(context) {
                            val providerServers = providerFakeServers.toMutableList()
                            val providerData = randomList(providerServers.size) {
                                val provider = providerServers.randomElement()
                                providerServers -= provider
                                randomProviderData(provider)
                            }
                            val possibleLibraries = libraries.filter { library ->
                                providerData.all { data ->
                                    val providerServer = providerFakeServers.find { it.id == data.providerId }!!
                                    library.platform in providerServer.supportedPlatforms
                                }
                            }
                            val path = randomPath(maxElements = 6, minElements = 3)
                            persistenceService.insertGame(
                                metadata = Metadata(
                                    libraryId = possibleLibraries.randomElement().id,
                                    path = path,
                                    timestamp = Timestamp.now
                                ),
                                providerData = providerData,
                                userData = UserData.Null
                            )
                        }
                    }.forEach { it.await() }
                }
            }
        }

        println("Initialized test db with $numGames games in ${timeTaken.humanReadable}")
    }

    private fun randomProviderData(provider: GameProviderFakeServer) = ProviderData(
        header = ProviderHeader(
            providerId = provider.id,
            providerGameId = provider.randomProviderGameId()
        ),
        gameData = GameData(
            name = randomName(),
            description = randomParagraph(),
            releaseDate = randomLocalDateString(),
            criticScore = randomScore(),
            userScore = randomScore(),
            genres = randomList(4) { randomGenre() },
            thumbnailUrl = provider.thumbnailUrl?.let { "$it/${randomWord()}.jpg".sometimesNull() },
            posterUrl = provider.posterUrl?.let { "$it/${randomWord()}.jpg".sometimesNull() },
            screenshotUrls = if (provider.screenshotUrl != null) randomList(10) { "${provider.screenshotUrl}/${randomWord()}.jpg" } else emptyList()
        ),
        siteUrl = randomUrl(),
        timestamp = Timestamp.now
    )

    private object TestModule : AbstractModule() {
        @Provides
        @Singleton
        fun config(): Config = ConfigFactory.load()

        @Provides
        @Singleton
        fun providerStorageFactory(): ProviderStorageFactory = ProviderStorageFactoryImpl()
    }
}

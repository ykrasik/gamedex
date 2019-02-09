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

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.common.FakeServerManager
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceConfig
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceServiceImpl
import com.gitlab.ykrasik.gamedex.test.*
import com.gitlab.ykrasik.gamedex.util.humanReadableDuration
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.Executors

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
        System.setProperty("gameDex.persistence.dbUrl", dbUrl)

        // Pre-Load test images
        randomImage()

        FakeServerManager.using {
            if (System.getProperty("gameDex.persistence.noGenerateDb") == null) {
                generateDb(
                    numGames = 5000
//                    numGames = null
                )
            }

            Main.main(args)
        }
    }

    private fun generateDb(numGames: Int?) {
        persistenceService.dropDb()
        if (numGames == null) return

        val start = System.currentTimeMillis()
        val basePath = File(".").absoluteFile.normalize()
        val libraries = listOf(
            Triple(LibraryType.Digital, Platform.Windows, "app"),
            Triple(LibraryType.Digital, Platform.Android, "build"),
            Triple(LibraryType.Digital, Platform.Mac, "conf"),
            Triple(LibraryType.Excluded, null, "core"),
            Triple(LibraryType.Digital, Platform.Windows, "provider")
        ).map { (type, platform, name) ->
            persistenceService.insertLibrary(LibraryData(name, basePath.resolve(name), type, platform))
        }.filter { it.type != LibraryType.Excluded }

        Executors.newFixedThreadPool(10).asCoroutineDispatcher().use { context ->
            runBlocking {
                (0 until numGames).map {
                    async(context) {
                        val providerServers = FakeServerManager.providerFakeServers.toMutableList()
                        val path = randomPath(maxElements = 6, minElements = 3)
                        persistenceService.insertGame(
                            metadata = com.gitlab.ykrasik.gamedex.Metadata(
                                libraryId = libraries.randomElement().id,
                                path = path,
                                timestamp = Timestamp.now
                            ),
                            providerData = randomList(providerServers.size) {
                                val provider = providerServers.randomElement()
                                providerServers -= provider
                                randomProviderData(provider)
                            },
                            userData = UserData.Null
                        )
                    }
                }.forEach { it.await() }
            }
        }

        println("Initialized test db with $numGames games in ${(System.currentTimeMillis() - start).humanReadableDuration}")
    }

    private fun randomProviderData(provider: GameProviderFakeServer) = ProviderData(
        header = ProviderHeader(
            id = provider.id,
            apiUrl = provider.apiDetailsUrl
        ),
        gameData = GameData(
            name = randomName(),
            description = randomParagraph(),
            releaseDate = randomLocalDateString(),
            criticScore = randomScore(),
            userScore = randomScore(),
            genres = randomList(4) { randomGenre() },
            imageUrls = imageUrls(provider)
        ),
        siteUrl = randomUrl(),
        timestamp = Timestamp.now
    )

    private fun imageUrls(server: GameProviderFakeServer) = ImageUrls(
        thumbnailUrl = server.thumbnailUrl?.let { "$it/${randomWord()}.jpg".sometimesNull() },
        posterUrl = server.posterUrl?.let { "$it/${randomWord()}.jpg".sometimesNull() },
        screenshotUrls = if (server.screenshotUrl != null) randomList(10) { "${server.screenshotUrl}/${randomWord()}.jpg" } else emptyList()
    )

    // 9/10 chance.
    private fun String.sometimesNull() = if (randomInt(10) < 10) this else null
}
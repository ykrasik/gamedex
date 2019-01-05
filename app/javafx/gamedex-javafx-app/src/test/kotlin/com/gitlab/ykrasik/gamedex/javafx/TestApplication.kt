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

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceConfig
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceServiceImpl
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombFakeServer
import com.gitlab.ykrasik.gamedex.provider.igdb.IgdbFakeServer
import com.gitlab.ykrasik.gamedex.test.*
import com.gitlab.ykrasik.gamedex.util.toHumanReadableDuration
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
    val giantBombServer = GiantBombFakeServer(9001, apiKey = "valid")
    val igdbServer = IgdbFakeServer(9002, apiKey = "valid")

    val dbUrl = "jdbc:h2:./test"
    val persistenceService by lazy { PersistenceServiceImpl(PersistenceConfig(dbUrl, "org.h2.Driver", "sa", "")) }

    @JvmStatic
    fun main(args: Array<String>) {
        System.setProperty("gameDex.persistence.dbUrl", dbUrl)
        System.setProperty("gameDex.provider.giantBomb.baseUrl", giantBombServer.baseUrl)
        System.setProperty("gameDex.provider.igdb.baseUrl", igdbServer.baseUrl)
        System.setProperty("gameDex.provider.igdb.baseImageUrl", igdbServer.baseImageUrl)

        // Pre-Load test images
        randomImage()

        giantBombServer.start().use {
            igdbServer.start().use {
                if (System.getProperty("gameDex.persistence.noGenerateDb") == null) {
                    generateDb(
                        numGames = 5000
//                        numGames = null
                    )
                }

                Main.main(args)
            }
        }
    }

    private fun generateDb(numGames: Int?) {
        persistenceService.dropDb()
        if (numGames == null) return

        val start = System.currentTimeMillis()
        val basePath = File(".").absoluteFile.normalize()
        val libraries = listOf(
            Platform.pc to "app",
            Platform.android to "build",
            Platform.mac to "conf",
            Platform.excluded to "core",
            Platform.pc to "provider"
        ).map { (platform, name) ->
            persistenceService.insertLibrary(LibraryData(name, basePath.resolve(name), platform))
        }.filter { it.platform != Platform.excluded }

        Executors.newFixedThreadPool(10).asCoroutineDispatcher().use { context ->
            runBlocking {
                (0 until numGames).map {
                    async(context) {
                        val providerIds = mutableListOf(giantBombServer.providerId, igdbServer.providerId)
                        val path = randomPath(maxElements = 6, minElements = 3)
                        persistenceService.insertGame(
                            metadata = com.gitlab.ykrasik.gamedex.Metadata(
                                libraryId = libraries.randomElement().id,
                                path = path,
                                timestamp = Timestamp.now
                            ),
                            providerData = randomList(providerIds.size) {
                                val providerId = providerIds.randomElement()
                                providerIds -= providerId
                                randomProviderData(providerId)
                            },
                            userData = UserData.Null
                        )
                    }
                }.forEach { it.await() }
            }
        }

        println("Initialized test db with $numGames games in ${(System.currentTimeMillis() - start).toHumanReadableDuration()}")
    }

    private fun randomProviderData(id: ProviderId) = ProviderData(
        header = ProviderHeader(
            id = id,
            apiUrl = apiUrl(id),
            timestamp = Timestamp.now
        ),
        gameData = GameData(
            siteUrl = randomUrl(),
            name = randomName(),
            description = randomParagraph(),
            releaseDate = randomLocalDateString(),
            criticScore = randomScore(),
            userScore = randomScore(),
            genres = randomList(4) { randomGenre() },
            imageUrls = imageUrls(id)
        )
    )

    private fun apiUrl(id: ProviderId) = when (id) {
        giantBombServer.providerId -> giantBombServer.apiDetailsUrl
        igdbServer.providerId -> igdbServer.detailsUrl(randomInt())
        else -> error("Invalid providerId: $id")
    }

    private fun imageUrls(id: ProviderId) = when (id) {
        giantBombServer.providerId -> randomGiantBombImageUrls()
        igdbServer.providerId -> randomIgdbImageUrls()
        else -> error("Invalid providerId: $id")
    }

    private fun randomGiantBombImageUrls() = ImageUrls(
        thumbnailUrl = "${giantBombServer.thumbnailUrl}/${randomWord()}.jpg".sometimesNull(),
        posterUrl = "${giantBombServer.superUrl}/${randomWord()}.jpg".sometimesNull(),
        screenshotUrls = randomList(10) { "${giantBombServer.screenshotUrl}/${randomWord()}.jpg" }
    )

    private fun randomIgdbImageUrls() = ImageUrls(
        thumbnailUrl = "${igdbServer.thumbnailUrl}/${randomWord()}.png".sometimesNull(),
        posterUrl = "${igdbServer.posterUrl}/${randomWord()}.png".sometimesNull(),
        screenshotUrls = randomList(10) { "${igdbServer.screenshotUrl}/${randomWord()}.png" }
    )

    // 9/10 chance.
    private fun String.sometimesNull() = if (randomInt(10) < 10) this else null
}
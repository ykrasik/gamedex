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

package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.core.file.NewDirectoryDetector
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceConfig
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceServiceImpl
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombFakeServer
import com.gitlab.ykrasik.gamedex.provider.igdb.IgdbFakeServer
import com.gitlab.ykrasik.gamedex.test.*
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
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

    @JvmStatic
    fun main(args: Array<String>) {
        System.setProperty("gameDex.persistence.dbUrl", "jdbc:h2:./test")
        System.setProperty("gameDex.provider.giantBomb.endpoint", giantBombServer.endpointUrl)
        System.setProperty("gameDex.provider.igdb.endpoint", igdbServer.endpointUrl)
        System.setProperty("gameDex.provider.igdb.baseImageUrl", igdbServer.baseImageUrl)
        System.setProperty("gameDex.newDirectoryDetector.class", StubNewDirectoryDetector::class.qualifiedName)

        // Pre-Load test images
        TestImages

        giantBombServer.start().use {
            igdbServer.start().use {
                generateDb()

                Main.main(args)
            }
        }
    }

    private fun generateDb() {
        val numGames = 500

//        val guice = GuiceDiContainer()
//        val persistenceService = guice.getInstance(PersistenceService::class)
        val persistenceService = PersistenceServiceImpl(PersistenceConfig("jdbc:h2:./test", "org.h2.Driver", "sa", ""))
        persistenceService.dropDb()

        val libraries = (1..5).zip(listOf(Platform.pc, Platform.android, Platform.mac, Platform.excluded, Platform.pc)).map { (i, platform) ->
            val name = "lib$i"
            persistenceService.insertLibrary(path = randomFile(), data = LibraryData(platform, name))
        }.filter { it.platform != Platform.excluded }

        val executor = Executors.newFixedThreadPool(10)
        val context = executor.asCoroutineDispatcher()
        runBlocking {
            (0 until numGames).map {
                async(context) {
                    val providerIds = mutableListOf(giantBombServer.providerId, igdbServer.providerId)
                    persistenceService.insertGame(
                        metadata = randomMetadata(libraries.randomElement().id),
                        providerData = List(rnd.nextInt(providerIds.size + 1)) {
                            val providerId = providerIds.randomElement()
                            providerIds -= providerId
                            randomProviderData(providerId)
                        },
                        userData = null
                    )
                }
            }.forEach { it.await() }
        }
        executor.shutdownNow()
    }

    private fun randomProviderData(id: ProviderId) = ProviderData(
        header = randomProviderHeader(id, apiUrl(id)),
        gameData = randomGameData(imageUrls(id))
    )

    private fun apiUrl(id: ProviderId) = when (id) {
        giantBombServer.providerId -> giantBombServer.apiDetailsUrl
        igdbServer.providerId -> igdbServer.detailsUrl(rnd.nextInt())
        else -> error("Invalid providerId: $id")
    }

    private fun imageUrls(id: ProviderId) = when (id) {
        giantBombServer.providerId -> randomGiantBombImageUrls()
        igdbServer.providerId -> randomIgdbImageUrls()
        else -> error("Invalid providerId: $id")
    }

    private fun randomGiantBombImageUrls() = ImageUrls(
        thumbnailUrl = "${giantBombServer.thumbnailUrl}/${randomString()}.jpg".sometimesNull(),
        posterUrl = "${giantBombServer.superUrl}/${randomString()}.jpg".sometimesNull(),
        screenshotUrls = List(rnd.nextInt(10)) { "${giantBombServer.screenshotUrl}/${randomString()}.jpg" }
    )

    private fun randomIgdbImageUrls() = ImageUrls(
        thumbnailUrl = "${igdbServer.thumbnailUrl}/${randomString()}.png".sometimesNull(),
        posterUrl = "${igdbServer.posterUrl}/${randomString()}.png".sometimesNull(),
        screenshotUrls = List(rnd.nextInt(10)) { "${igdbServer.screenshotUrl}/${randomString()}.png" }
    )

    // 9/10 chance.
    private fun String.sometimesNull() = if (rnd.nextInt(10) < 9) this else null
}

class StubNewDirectoryDetector : NewDirectoryDetector {
    override fun detectNewDirectories(dir: File, excludedDirectories: Set<File>) =
        List(rnd.nextInt(4)) { randomFile() }
}
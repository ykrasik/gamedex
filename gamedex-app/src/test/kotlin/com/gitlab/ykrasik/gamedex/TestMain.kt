package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.common.datamodel.GamePlatform
import com.gitlab.ykrasik.gamedex.common.datamodel.LibraryData
import com.gitlab.ykrasik.gamedex.common.util.ConfigProvider
import com.gitlab.ykrasik.gamedex.common.util.defaultConfig
import com.gitlab.ykrasik.gamedex.common.util.toFile
import com.gitlab.ykrasik.gamedex.persistence.AddLibraryRequest
import com.gitlab.ykrasik.gamedex.persistence.DbInitializer
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.ui.view.fragment.randomAddGameRequest
import com.gitlab.ykrasik.gamedex.ui.view.fragment.randomGameImage
import com.typesafe.config.ConfigValueFactory
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking

/**
 * User: ykrasik
 * Date: 18/03/2017
 * Time: 21:59
 */
object TestMain {
    @JvmStatic fun main(args: Array<String>) {
        ConfigProvider.setConfig(defaultConfig
            .withValue("gameDex.persistence.dbUrl", ConfigValueFactory.fromAnyRef("jdbc:h2:./test"))
        )

//        generateDb(1000)

        Main.main(args)
    }

    private fun generateDb(numGames: Int) {
        Main.GuiceDiContainer.getInstance(DbInitializer::class).reload()
        
        val persistenceService = Main.GuiceDiContainer.getInstance(PersistenceService::class)

        persistenceService.insert(AddLibraryRequest(
            path = javaClass.getResource("/test-games").toURI().toFile(), data = LibraryData(GamePlatform.pc, "Test Games")
        ))

        val context = newFixedThreadPoolContext(10, "db-generator")
        runBlocking {
            (0..numGames-1).map {
                async(context) {
                    val game = persistenceService.insert(randomAddGameRequest())
                    persistenceService.updateImage(randomGameImage(game.imageIds.thumbnailId!!))
                }
            }.forEach { it.await() }
        }
    }
}
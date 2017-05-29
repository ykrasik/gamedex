package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.test.randomElement
import com.gitlab.ykrasik.gamedex.test.rnd

/**
 * User: ykrasik
 * Date: 13/05/2017
 * Time: 12:12
 */
object EditGameDataFragmentTestApp : BaseTestApp() {
    override fun init() {
        val providerData = GameProviderType.values().map { type ->
            ProviderData(
                header = randomProviderHeader(type),
                gameData = randomGameData(),
                imageUrls = randomImageUrls()
            )
        }
        val game = Game(
            rawGame = RawGame(
                id = rnd.nextInt(),
                metaData = randomMetaData(),
                providerData = providerData,
                userData = null
            ),
            library = randomLibrary(),
            gameData = providerData.randomElement().gameData,
            providerHeaders = randomProviderHeaders(),
            imageUrls = randomImageUrls()
        )
        println("Result: " + EditGameDataFragment(game, initialTab = GameDataType.thumbnail).show())
        System.exit(0)
    }

    @JvmStatic fun main(args: Array<String>) {}
}
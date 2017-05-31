package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.test.randomElement
import com.gitlab.ykrasik.gamedex.test.rnd

/**
 * User: ykrasik
 * Date: 13/05/2017
 * Time: 12:12
 */
object TagFragmentTestApp : BaseTestApp() {
    override fun init() {
        val providerData = testProviderIds.map { id ->
            ProviderData(
                header = randomProviderHeader(id),
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
        println("Result: " + TagFragment(game).show())
        System.exit(0)
    }

    @JvmStatic fun main(args: Array<String>) {}
}
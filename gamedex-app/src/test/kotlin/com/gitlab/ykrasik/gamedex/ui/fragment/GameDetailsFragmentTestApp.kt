package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.test.rnd

/**
 * User: ykrasik
 * Date: 01/04/2017
 * Time: 14:06
 */
object GameDetailsFragmentTestApp : BaseTestApp() {
    override fun init() {
        val game = Game(
            rawGame = RawGame(
                id = rnd.nextInt(),
                metaData = randomMetaData(),
                providerData = emptyList(),
                userData = null
            ),
            library = randomLibrary(),
            gameData = randomGameData(),
            providerHeaders = randomProviderHeaders(),
            imageUrls = randomImageUrls()
        )
        println("Result: " + GameDetailsFragment(game, displayVideos = false).show())
        System.exit(0)
    }

    @JvmStatic fun main(args: Array<String>) {}
}
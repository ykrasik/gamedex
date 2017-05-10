package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.test.*

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
            library = Library(
                id = rnd.nextInt(),
                path = randomFile(),
                data = LibraryData(
                    platform = randomEnum(),
                    name = randomName()
                )
            ),
            gameData = randomGameData(),
            providerHeaders = randomProviderHeaders(),
            imageUrls = ImageUrls(
                thumbnailUrl = randomUrl(),
                posterUrl = randomUrl(),
                screenshotUrls = List(rnd.nextInt(10)) { randomUrl() }
            )
        )
        println("Result: " + GameDetailsFragment(game, displayVideos = false).show())
        System.exit(0)
    }

    @JvmStatic fun main(args: Array<String>) {}
}
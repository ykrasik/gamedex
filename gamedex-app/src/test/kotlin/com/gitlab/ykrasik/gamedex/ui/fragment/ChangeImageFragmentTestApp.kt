package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.test.randomUrl
import com.gitlab.ykrasik.gamedex.test.rnd

/**
 * User: ykrasik
 * Date: 13/05/2017
 * Time: 12:12
 */
object ChangeImageFragmentTestApp : BaseTestApp() {
    override fun init() {
        val game = Game(
            rawGame = RawGame(
                id = rnd.nextInt(),
                metaData = randomMetaData(),
                providerData = GameProviderType.values().map { type ->
                    ProviderData(
                        header = ProviderHeader(
                            type = type,
                            apiUrl = randomUrl(),
                            siteUrl = randomUrl()
                        ),
                        gameData = randomGameData(),
                        imageUrls = randomImageUrls()
                    )
                },
                userData = null
            ),
            library = randomLibrary(),
            gameData = randomGameData(),
            providerHeaders = randomProviderHeaders(),
            imageUrls = randomImageUrls()
        )
        println("Result: " + ChangeImageFragment.thumbnail(game).show())
        System.exit(0)
    }

    @JvmStatic fun main(args: Array<String>) {}
}
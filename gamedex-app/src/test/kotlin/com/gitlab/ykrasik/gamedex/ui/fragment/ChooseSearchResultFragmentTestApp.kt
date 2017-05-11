package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.BaseTestApp
import com.gitlab.ykrasik.gamedex.GameProviderType
import com.gitlab.ykrasik.gamedex.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.core.SearchChooser
import com.gitlab.ykrasik.gamedex.test.*

/**
 * User: ykrasik
 * Date: 12/03/2017
 * Time: 13:05
 */
object ChooseSearchResultFragmentTestApp : BaseTestApp() {
    override fun init() {
        fun randomSearchResult() = ProviderSearchResult(
            name = randomName(),
            releaseDate = randomLocalDate(),
            score = randomScore(),
            thumbnailUrl = randomUrl(),
            apiUrl = randomUrl()
        )

        val data = SearchChooser.Data(
            name = randomName(),
            path = randomFile(),
            providerType = GameProviderType.GiantBomb,
            results = List(10) { randomSearchResult() },
            filteredResults = List(10) { randomSearchResult() }
        )
        println("Result: " + ChooseSearchResultFragment(data).show())
        System.exit(0)
    }

    @JvmStatic fun main(args: Array<String>) {}
}
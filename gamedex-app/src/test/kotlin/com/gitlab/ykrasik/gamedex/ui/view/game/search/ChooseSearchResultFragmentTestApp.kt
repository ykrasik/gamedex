package com.gitlab.ykrasik.gamedex.ui.view.game.search

import com.gitlab.ykrasik.gamedex.BaseFragmentTestApp
import com.gitlab.ykrasik.gamedex.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.core.SearchChooser
import com.gitlab.ykrasik.gamedex.test.*

/**
 * User: ykrasik
 * Date: 12/03/2017
 * Time: 13:05
 */
object ChooseSearchResultFragmentTestApp : BaseFragmentTestApp() {
    override fun init() {
        fun randomSearchResult() = ProviderSearchResult(
            name = randomName(),
            releaseDate = randomLocalDateString(),
            criticScore = randomScore(),
            userScore = randomScore(),
            thumbnailUrl = randomUrl(),
            apiUrl = randomUrl()
        )

        val data = SearchChooser.Data(
            name = randomName(),
            path = randomFile(),
            platform = randomEnum(),
            providerId = testProviderIds.randomElement(),
            results = List(10) { randomSearchResult() },
            filteredResults = List(10) { randomSearchResult() }
        )
        println("Result: " + SearchResultsFragment(data).show())
    }

    @JvmStatic fun main(args: Array<String>) {}
}
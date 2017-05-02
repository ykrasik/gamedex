package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.BaseTestApp
import com.gitlab.ykrasik.gamedex.GameProviderType
import com.gitlab.ykrasik.gamedex.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.core.ChooseSearchResultData
import com.gitlab.ykrasik.gamedex.test.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

/**
 * User: ykrasik
 * Date: 12/03/2017
 * Time: 13:05
 */
object ChooseSearchResultFragmentTestApp : BaseTestApp() {
    override fun init() {
        launch(CommonPool) {
            val data = ChooseSearchResultData(
                name = randomName(),
                path = randomFile(),
                providerType = GameProviderType.GiantBomb,
                isNewSearch = true,
                searchResults = List(10) {
                    ProviderSearchResult(
                        name = randomName(),
                        releaseDate = randomLocalDate(),
                        score = randomScore(),
                        thumbnailUrl = randomUrl(),
                        apiUrl = randomUrl()
                    )
                }
            )
            println("Result: " + ChooseSearchResultFragment(data).show())
            System.exit(0)
        }
    }

    @JvmStatic fun main(args: Array<String>) {}
}
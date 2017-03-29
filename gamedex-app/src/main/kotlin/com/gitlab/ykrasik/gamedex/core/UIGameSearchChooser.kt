package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.provider.ChooseSearchResultData
import com.gitlab.ykrasik.gamedex.provider.GameSearchChooser
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.SearchResultChoice
import com.gitlab.ykrasik.gamedex.ui.view.fragment.ChooseSearchResultFragment
import com.gitlab.ykrasik.gamedex.ui.view.fragment.ProviderSearchResultWithThumbnail
import tornadofx.observable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 30/12/2016
 * Time: 22:03
 */
@Singleton
class UIGameSearchChooser @Inject constructor(private val imageLoader: ImageLoader) : GameSearchChooser {
    override suspend fun choose(data: ChooseSearchResultData): SearchResultChoice {
        val resultsWithThumbnail = data.searchResults.map { it.addThumbnail() }.observable()
        return ChooseSearchResultFragment(
            searchedName = data.name,
            path = data.path,
            info = data.info,
            searchResults = resultsWithThumbnail,
            canProceedWithout = data.canProceedWithout
        ).show()
    }

    private fun ProviderSearchResult.addThumbnail(): ProviderSearchResultWithThumbnail {
        // TODO: Cancel running download jobs when search result screen is closed.
        val thumbnail = imageLoader.downloadImage(thumbnailUrl)
        return ProviderSearchResultWithThumbnail(this, thumbnail)
    }
}
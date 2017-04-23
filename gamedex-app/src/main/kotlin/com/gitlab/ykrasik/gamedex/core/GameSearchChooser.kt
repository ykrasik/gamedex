package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.GameProviderType
import com.gitlab.ykrasik.gamedex.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.ui.fragment.ChooseSearchResultFragment
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 30/12/2016
 * Time: 22:03
 */
interface GameSearchChooser {
    suspend fun choose(data: ChooseSearchResultData): SearchResultChoice
}

@Singleton
class UIGameSearchChooser @Inject constructor() : GameSearchChooser {
    override suspend fun choose(data: ChooseSearchResultData): SearchResultChoice =
        ChooseSearchResultFragment(data).show()
}

data class ChooseSearchResultData(
    val name: String,
    val path: File,
    val providerType: GameProviderType,
    val searchResults: List<ProviderSearchResult>
)
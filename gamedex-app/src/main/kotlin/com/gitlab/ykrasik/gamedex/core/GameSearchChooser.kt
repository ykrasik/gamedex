package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.DataProviderInfo
import com.gitlab.ykrasik.gamedex.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.ui.view.fragment.ChooseSearchResultFragment
import com.gitlab.ykrasik.gamedex.util.UserPreferences
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
class UIGameSearchChooser @Inject constructor(private val userPreferences: UserPreferences) : GameSearchChooser {
    override suspend fun choose(data: ChooseSearchResultData): SearchResultChoice {
        return if (userPreferences.handsFreeMode) {
            if (data.searchResults.size == 1) {
                SearchResultChoice.Ok(data.searchResults.first())
            } else {
                SearchResultChoice.Cancel
            }
        } else {
            ChooseSearchResultFragment(data).show()
        }
    }
}

data class ChooseSearchResultData(
    val name: String,
    val path: File,
    val info: DataProviderInfo,
    val searchResults: List<ProviderSearchResult>
)

sealed class SearchResultChoice {
    data class Ok(val result: ProviderSearchResult) : SearchResultChoice()
    data class NewSearch(val newSearch: String) : SearchResultChoice()
    object Cancel : SearchResultChoice()
    object ProceedWithout : SearchResultChoice()
}
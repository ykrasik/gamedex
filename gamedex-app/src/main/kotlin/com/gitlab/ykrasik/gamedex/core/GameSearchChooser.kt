package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.provider.ChooseSearchResultData
import com.gitlab.ykrasik.gamedex.provider.SearchResultChoice
import com.gitlab.ykrasik.gamedex.ui.view.fragment.ChooseSearchResultFragment
import com.gitlab.ykrasik.gamedex.util.UserPreferences
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
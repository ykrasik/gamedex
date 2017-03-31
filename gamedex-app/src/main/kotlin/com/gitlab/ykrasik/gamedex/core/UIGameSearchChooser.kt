package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.provider.ChooseSearchResultData
import com.gitlab.ykrasik.gamedex.provider.GameSearchChooser
import com.gitlab.ykrasik.gamedex.provider.SearchResultChoice
import com.gitlab.ykrasik.gamedex.ui.view.fragment.ChooseSearchResultFragment
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 30/12/2016
 * Time: 22:03
 */
@Singleton
class UIGameSearchChooser @Inject constructor() : GameSearchChooser {
    override suspend fun choose(data: ChooseSearchResultData): SearchResultChoice {
        return ChooseSearchResultFragment(data).show()
    }
}
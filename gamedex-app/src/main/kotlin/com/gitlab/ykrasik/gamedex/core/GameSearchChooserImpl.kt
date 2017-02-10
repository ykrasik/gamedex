package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.provider.DataProviderInfo
import com.gitlab.ykrasik.gamedex.provider.GameSearchChooser
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.SearchContext
import com.gitlab.ykrasik.gamedex.ui.view.fragment.ChooseSearchResultFragment
import tornadofx.observable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 30/12/2016
 * Time: 22:03
 */
@Singleton
class GameSearchChooserImpl @Inject constructor() : GameSearchChooser {
    override fun choose(info: DataProviderInfo, providerSearchResults: List<ProviderSearchResult>, context: SearchContext): ProviderSearchResult? {
        return ChooseSearchResultFragment(context, info, providerSearchResults.observable()).show()
    }
}
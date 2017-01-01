package com.gitlab.ykrasik.gamedex.core.scan

import com.gitlab.ykrasik.gamedex.provider.GameSearchChooser
import com.gitlab.ykrasik.gamedex.provider.SearchContext
import com.gitlab.ykrasik.gamedex.provider.SearchResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 30/12/2016
 * Time: 22:03
 */
@Singleton
class GameSearchChooserImpl @Inject constructor() : GameSearchChooser {
    override fun choose(results: List<SearchResult>, context: SearchContext): SearchResult? {
        // TODO: Display dialog
        return results.firstOrNull()
    }
}
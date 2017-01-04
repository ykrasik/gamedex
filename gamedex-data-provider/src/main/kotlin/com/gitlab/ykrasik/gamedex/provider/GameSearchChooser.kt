package com.gitlab.ykrasik.gamedex.provider

import java.nio.file.Path

/**
 * User: ykrasik
 * Date: 30/12/2016
 * Time: 22:01
 */
interface GameSearchChooser {
    fun choose(info: DataProviderInfo, providerSearchResults: List<ProviderSearchResult>, context: SearchContext): ProviderSearchResult?
}

class SearchContext(val searchedName: String, val path: Path) {
    val discardedResults = setOf<ProviderSearchResult>()
}
package com.gitlab.ykrasik.gamedex.provider

import java.io.File

/**
 * User: ykrasik
 * Date: 30/12/2016
 * Time: 22:01
 */
interface GameSearchChooser {
    fun choose(info: DataProviderInfo, searchResults: List<ProviderSearchResult>, context: SearchContext): ProviderSearchResult?
}

class SearchContext(val searchedName: String, val path: File) {
    val discardedResults = setOf<ProviderSearchResult>()
}
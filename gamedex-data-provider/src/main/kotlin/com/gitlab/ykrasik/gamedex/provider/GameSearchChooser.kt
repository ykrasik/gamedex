package com.gitlab.ykrasik.gamedex.provider

/**
 * User: ykrasik
 * Date: 30/12/2016
 * Time: 22:01
 */
interface GameSearchChooser {
    fun choose(results: List<SearchResult>, context: SearchContext): SearchResult?
}

class SearchContext {
    val discardedResults = setOf<SearchResult>()
}
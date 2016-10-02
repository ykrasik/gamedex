package com.gitlab.ykrasik.gamedex.provider

import com.github.ykrasik.gamedex.datamodel.GamePlatform

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:42
 */
interface DataProvider {
    val info: DataProviderInfo

    fun search(name: String, platform: GamePlatform): List<SearchResult>

    fun fetch(searchResult: SearchResult): ProviderGameData
}
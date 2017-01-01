package com.gitlab.ykrasik.gamedex.provider

import com.github.ykrasik.gamedex.datamodel.DataProviderType
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.ImageData

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

class DataProviderInfo(
    val name: String,
    val type: DataProviderType,
    val logo: ImageData
)
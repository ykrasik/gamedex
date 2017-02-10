package com.gitlab.ykrasik.gamedex.provider

import com.gitlab.ykrasik.gamedex.common.datamodel.DataProviderType
import com.gitlab.ykrasik.gamedex.common.datamodel.GamePlatform
import javafx.scene.image.Image

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:42
 */
interface DataProvider {
    val info: DataProviderInfo

    fun search(name: String, platform: GamePlatform): List<ProviderSearchResult>

    // TODO: Find a way for providers to fetch from their native SearchResults, and not from the generic one.
    fun fetch(searchResult: ProviderSearchResult): ProviderFetchResult
}

class DataProviderInfo(
    val name: String,
    val type: DataProviderType,
    val logo: Image // TODO: Measure the performance cost of returning a byteArray and constructing an image out of it every time instead.
)


package com.gitlab.ykrasik.gamedex.provider

import com.github.ykrasik.gamedex.datamodel.DataProviderType
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import javafx.scene.image.Image

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:42
 */
interface DataProvider {
    val info: DataProviderInfo

    fun search(name: String, platform: GamePlatform): List<ProviderSearchResult>

    fun fetch(searchResult: ProviderSearchResult): ProviderFetchResult
}

class DataProviderInfo(
    val name: String,
    val type: DataProviderType,
    val logo: Image // TODO: Measure the performance cost of returning a byteArray and constructing an image out of it every time instead.
)
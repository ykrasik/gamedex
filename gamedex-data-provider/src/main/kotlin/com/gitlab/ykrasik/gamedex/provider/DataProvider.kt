package com.gitlab.ykrasik.gamedex.provider

import com.github.ykrasik.gamedex.datamodel.DataProviderType
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.ProviderGameData
import javafx.scene.image.Image
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:42
 */
interface DataProvider {
    val info: DataProviderInfo

    fun search(name: String, platform: GamePlatform): List<ProviderSearchResult>

    fun fetch(searchResult: ProviderSearchResult): ProviderGameData
}

class DataProviderInfo(
    val name: String,
    val type: DataProviderType,
    val logo: Image // TODO: Measure the performance cost of returning a byteArray and constructing an image out of it every time instead.
)

data class ProviderSearchResult(
    val detailUrl: String,
    val name: String,
    val releaseDate: LocalDate?,
    val score: Double?,
    val thumbnailUrl: String?
)
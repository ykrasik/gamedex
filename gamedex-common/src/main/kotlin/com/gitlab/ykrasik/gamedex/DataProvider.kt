package com.gitlab.ykrasik.gamedex

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

    fun fetch(apiUrl: String, platform: GamePlatform): ProviderFetchResult
}

enum class DataProviderType(val basicDataPriority: Int, val scorePriority: Int, val imagePriorty: Int) {
    Igdb(basicDataPriority = 2, scorePriority = 1, imagePriorty = 2),
    GiantBomb(basicDataPriority = 1, scorePriority = 999, imagePriorty = 1)
}

class DataProviderInfo(
    val name: String,
    val type: DataProviderType,
    val logo: Image // TODO: Measure the performance cost of returning a byteArray and constructing an image out of it every time instead.
)

data class ProviderSearchResult(
    val name: String,
    val releaseDate: LocalDate?,
    val score: Double?,
    val thumbnailUrl: String?,
    val apiUrl: String
)

data class ProviderFetchResult(
    val providerData: ProviderData,
    val gameData: GameData,
    val imageUrls: ImageUrls
)
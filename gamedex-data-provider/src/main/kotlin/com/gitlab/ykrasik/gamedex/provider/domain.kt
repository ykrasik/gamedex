package com.gitlab.ykrasik.gamedex.provider

import com.gitlab.ykrasik.gamedex.common.datamodel.GameData
import com.gitlab.ykrasik.gamedex.common.datamodel.ImageUrls
import com.gitlab.ykrasik.gamedex.common.datamodel.ProviderData
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 08/02/2017
 * Time: 17:33
 */
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

data class ProviderGame(
    val gameData: GameData,
    val imageUrls: ImageUrls,
    val providerData: List<ProviderData>
)
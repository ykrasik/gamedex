package com.gitlab.ykrasik.gamedex.provider

import com.github.ykrasik.gamedex.common.datamodel.GameData
import com.github.ykrasik.gamedex.common.datamodel.ImageData
import com.github.ykrasik.gamedex.common.datamodel.ProviderData
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
    val imageData: ImageData
)

data class ProviderGame(
    val gameData: GameData,
    val imageData: ImageData,
    val providerData: List<ProviderData>
)
package com.gitlab.ykrasik.gamedex.provider.igdb

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.ProviderOrderPriorities
import com.typesafe.config.Config
import io.github.config4k.extract

/**
 * User: ykrasik
 * Date: 29/01/2017
 * Time: 11:11
 */
data class IgdbConfig(
    val endpoint: String,
    val baseImageUrl: String,
    val accountUrl: String,
    val maxSearchResults: Int,
    val thumbnailImageType: IgdbProvider.IgdbImageType,
    val posterImageType: IgdbProvider.IgdbImageType,
    val screenshotImageType: IgdbProvider.IgdbImageType,
    val defaultOrder: ProviderOrderPriorities,
    private val platforms: Map<String, Int>,
    private val genres: Map<String, String>
) {
    private val _platforms = platforms.mapKeys { Platform.valueOf(it.key) }
    fun getPlatformId(platform: Platform) = _platforms[platform]!!

    private val _genres = genres.mapKeys { it.key.toInt() }
    fun getGenreName(genreId: Int): String = _genres[genreId]!!

    companion object {
        operator fun invoke(config: Config): IgdbConfig = config.extract("gameDex.provider.igdb")
    }
}
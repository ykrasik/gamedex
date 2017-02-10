package com.gitlab.ykrasik.gamedex.provider.igdb

import com.gitlab.ykrasik.gamedex.common.datamodel.GamePlatform
import com.gitlab.ykrasik.gamedex.common.util.defaultConfig
import com.gitlab.ykrasik.gamedex.common.util.getObjectMap
import com.typesafe.config.Config

/**
 * User: ykrasik
 * Date: 29/01/2017
 * Time: 11:11
 */
data class IgdbConfig(
    val apiKey: String,
    private val platforms: Map<GamePlatform, Int>,
    private val genres: Map<Int, String>
) {
    fun getPlatformId(platform: GamePlatform): Int = platforms[platform]!!
    fun getGenreName(genreId: Int): String = genres[genreId]!!

    companion object {
        operator fun invoke(config: Config = defaultConfig): IgdbConfig =
            config.getConfig("gameDex.provider.igdb").let { config ->
                IgdbConfig(
                    apiKey = config.getString("apiKey"),
                    platforms = config.getObjectMap("platforms", { GamePlatform.valueOf(it) }, { it as Int }),
                    genres = config.getObjectMap("genres", String::toInt, Any::toString)
                )
            }
    }
}
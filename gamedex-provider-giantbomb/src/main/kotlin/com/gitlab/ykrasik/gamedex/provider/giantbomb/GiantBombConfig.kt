package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.util.getObjectMap
import com.typesafe.config.Config

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:53
 */
data class GiantBombConfig(
    val endpoint: String,
    val apiKey: String,
    private val platforms: Map<Platform, Int>
) {
    fun getPlatformId(platform: Platform) = platforms[platform]!!

    companion object {
        @Suppress("NAME_SHADOWING")
        operator fun invoke(config: Config): GiantBombConfig = config.getConfig("gameDex.provider.giantBomb").let { config ->
            GiantBombConfig(
                endpoint = config.getString("endpoint"),
                apiKey = config.getString("apiKey"),
                platforms = config.getObjectMap("platforms", { Platform.valueOf(it) }, { it as Int })
            )
        }
    }
}
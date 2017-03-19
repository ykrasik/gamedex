package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.gitlab.ykrasik.gamedex.common.datamodel.GamePlatform
import com.gitlab.ykrasik.gamedex.common.util.ConfigProvider
import com.gitlab.ykrasik.gamedex.common.util.getObjectMap
import com.typesafe.config.Config

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:53
 */
data class GiantBombConfig(
    val endpoint: String,
    val applicationKey: String,
    private val platforms: Map<GamePlatform, Int>
) {
    fun getPlatformId(platform: GamePlatform) = platforms[platform]!!

    companion object {
        operator fun invoke(config: Config = ConfigProvider.config): GiantBombConfig =
            config.getConfig("gameDex.provider.giantBomb").let { config ->
                GiantBombConfig(
                    endpoint = config.getString("endpoint"),
                    applicationKey = config.getString("applicationKey"),
                    platforms = config.getObjectMap("platforms", { GamePlatform.valueOf(it) }, { it as Int })
                )
            }
    }
}
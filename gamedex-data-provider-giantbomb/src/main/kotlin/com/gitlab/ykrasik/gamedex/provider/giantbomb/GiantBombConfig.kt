package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.github.ykrasik.gamedex.common.BaseConfig
import com.github.ykrasik.gamedex.common.getObjectMap
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.typesafe.config.Config
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:53
 */
interface GiantBombConfig {
    val applicationKey: String

    fun getPlatformId(platform: GamePlatform): Int
}

@Singleton
class GiantBombConfigImpl @Inject constructor(c: Config) : BaseConfig(c, "gameDex.provider.giantBomb"), GiantBombConfig {
    override val applicationKey = config.getString("applicationKey")

    private val platforms = config.getObjectMap("platforms", { GamePlatform.valueOf(it) } , { it as Int })

    override fun getPlatformId(platform: GamePlatform) = platforms[platform]!!
}
package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.ProviderOrderPriorities
import com.typesafe.config.Config
import io.github.config4k.extract

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:53
 */
data class GiantBombConfig(
    val endpoint: String,
    val noImageFileName: String,
    val accountUrl: String,
    val defaultOrder: ProviderOrderPriorities,
    private val platforms: Map<String, Int>
) {
    private val _platforms = platforms.mapKeys { Platform.valueOf(it.key) }
    fun getPlatformId(platform: Platform) = _platforms[platform]!!

    companion object {
        operator fun invoke(config: Config): GiantBombConfig = config.extract("gameDex.provider.giantBomb")
    }
}
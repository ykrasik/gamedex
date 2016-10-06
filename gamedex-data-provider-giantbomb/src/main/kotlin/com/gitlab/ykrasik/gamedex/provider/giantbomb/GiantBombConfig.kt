package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.github.ykrasik.gamedex.datamodel.GamePlatform

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:53
 */
data class GiantBombConfig(
    val applicationKey: String,
    private val platforms: Map<GamePlatform, Int>
) {
    fun getPlatformId(platform: GamePlatform) = platforms[platform]!!
}
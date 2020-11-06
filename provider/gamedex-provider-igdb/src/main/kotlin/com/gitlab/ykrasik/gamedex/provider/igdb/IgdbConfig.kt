/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.provider.igdb

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.typesafe.config.Config
import io.github.config4k.extract

/**
 * User: ykrasik
 * Date: 29/01/2017
 * Time: 11:11
 */
data class IgdbConfig(
    val baseUrl: String,
    val baseImageUrl: String,
    val oauthUrl: String,
    val oauthTokenExpirationBufferSeconds: Int,
    val accountUrl: String,
    val thumbnailImageType: IgdbProvider.IgdbImageType,
    val posterImageType: IgdbProvider.IgdbImageType,
    val screenshotImageType: IgdbProvider.IgdbImageType,
    val defaultOrder: GameProvider.OrderPriorities,
    private val platforms: Map<String, Int>,
    private val genres: Map<String, String>
) {
    private val _platforms = platforms.mapKeys { Platform.valueOf(it.key) }
    fun getPlatformId(platform: Platform) = _platforms.getValue(platform)

    private val _genres = genres.mapKeys { it.key.toInt() }
    fun getGenreName(genreId: Int): String = _genres.getValue(genreId)

    companion object {
        operator fun invoke(config: Config): IgdbConfig = config.extract("gameDex.provider.igdb")
    }
}
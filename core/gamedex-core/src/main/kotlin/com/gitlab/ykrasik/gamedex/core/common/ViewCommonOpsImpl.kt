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

package com.gitlab.ykrasik.gamedex.core.common

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.common.ViewCommonOps
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.image.ImageService
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.version.ApplicationVersion
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 19/01/2019
 * Time: 22:26
 */
@Singleton
class ViewCommonOpsImpl @Inject constructor(
    private val config: CommonOpsConfig,
    private val imageService: ImageService,
    gameProviderService: GameProviderService,
    private val fileSystemService: FileSystemService,
) : ViewCommonOps {
    override val applicationVersion = ApplicationVersion

    override suspend fun fetchImage(url: String, persist: Boolean) = imageService.fetchImage(url, persist)

    override fun fetchFileTree(game: Game) = fileSystemService.fileTree(game.id, game.path)

    override val providers = gameProviderService.allProviders
    override val providerLogos = gameProviderService.logos

    override fun youTubeGameplayUrl(name: String, platform: Platform): String {
        val search = URLEncoder.encode("$name gameplay $platform", "utf-8")
        return "${config.youTubeBaseUrl}/results?search_query=$search"
    }
}

data class CommonOpsConfig(
    val youTubeBaseUrl: String,
)
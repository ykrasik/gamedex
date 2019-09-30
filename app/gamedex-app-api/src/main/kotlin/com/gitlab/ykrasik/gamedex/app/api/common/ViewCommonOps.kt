/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.api.common

import com.gitlab.ykrasik.gamedex.FileTree
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.Version
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.Ref

/**
 * User: ykrasik
 * Date: 19/01/2019
 * Time: 22:23
 *
 * Provided to the view layer through DI, as a more natural way of getting common data than through implementing interfaces.
 */
interface ViewCommonOps {
    val applicationVersion: Version

    suspend fun fetchImage(url: String, persist: Boolean): Image?

    fun fetchFileTree(game: Game): Ref<FileTree?>

    val providers: List<GameProvider.Metadata>
    val providerLogos: Map<ProviderId, Image>

    fun youTubeGameplayUrl(name: String, platform: Platform): String
}
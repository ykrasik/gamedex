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

package com.gitlab.ykrasik.gamedex.core.image

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.util.FileSize

/**
 * User: ykrasik
 * Date: 05/04/2018
 * Time: 11:05
 */
interface ImageService {
    fun createImage(data: ByteArray): Image

    suspend fun fetchThumbnail(game: Game): Image?
    suspend fun fetchPoster(game: Game): Image?

    suspend fun downloadImage(url: String): Image?

    fun fetchImageSizesExcept(exceptUrls: List<String>): Map<String, FileSize>

    fun deleteImages(imageUrls: List<String>)
}
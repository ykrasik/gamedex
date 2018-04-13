/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.api.image

import com.gitlab.ykrasik.gamedex.util.FileSize
import kotlinx.coroutines.experimental.Deferred

/**
 * User: ykrasik
 * Date: 05/04/2018
 * Time: 09:27
 *
 * [fetchImage] & [downloadImage] are meant to be called only by the ui thread.
 */
interface ImageRepository {
    // TODO: gameId is only here in order to link the url to a game so the image is auto-deleted with the game. Can also do this manually.
    fun fetchImage(url: String, gameId: Int, persistIfAbsent: Boolean): Deferred<ByteArray>

    fun downloadImage(url: String): Deferred<ByteArray>

    fun fetchImagesExcept(exceptUrls: List<String>): List<Pair<String, FileSize>>

    fun deleteImages(imageUrls: List<String>)
}
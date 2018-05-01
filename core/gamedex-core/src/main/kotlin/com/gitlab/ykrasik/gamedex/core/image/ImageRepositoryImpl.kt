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

package com.gitlab.ykrasik.gamedex.core.image

import com.gitlab.ykrasik.gamedex.core.api.image.ImageRepository
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.util.FileSize
import com.gitlab.ykrasik.gamedex.util.download
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 05/04/2018
 * Time: 11:05
 *
 * This is not a high-throughput repository. It is mostly meant to be called only by the ui thread.
 */
@Singleton
class ImageRepositoryImpl @Inject constructor(
    private val persistenceService: PersistenceService,
    config: ImageConfig
) : ImageRepository {
    private val persistedImageCache = Cache<String, Deferred<ByteArray>>(config.fetchCacheSize)
    private val downloadedImageCache = Cache<String, Deferred<ByteArray>>(config.downloadCacheSize)

    // Only meant to be accessed by the ui thread.
    // We disregard the value of persistIfAbsent when we get a cache hit, because
    // if it's already in the cache then it was already called with the exact same 'persistIfAbsent'.
    // A situation where this method is called once with persistIfAbsent=false and again
    // with the same url but persistIfAbsent=true simply doesn't exist.
    override fun fetchImage(url: String, gameId: Int, persistIfAbsent: Boolean): Deferred<ByteArray> =
        persistedImageCache.getOrPut(url) {
            async(CommonPool) {
                val persistedBytes = persistenceService.fetchImage(url)
                if (persistedBytes != null) {
                    persistedBytes
                } else {
                    val downloadedBytes = doDownload(url).await()
                    if (persistIfAbsent) {
                        // Save downloaded image as a fire-and-forget operation.
                        launch(CommonPool) {
                            persistenceService.insertImage(gameId, url, downloadedBytes)
                        }
                    }
                    downloadedBytes
                }
            }
        }

    // Only meant to be accessed by the ui thread.
    override fun downloadImage(url: String): Deferred<ByteArray> =
        downloadedImageCache.getOrPut(url) {
            doDownload(url)
        }

    private fun doDownload(url: String) = downloadedImageCache.getOrPut(url) {
        async(CommonPool) {
            download(url)
        }
    }

    override fun fetchImagesExcept(exceptUrls: List<String>): List<Pair<String, FileSize>> =
        persistenceService.fetchImageSizesExcept(exceptUrls).map { (url, size) -> url to FileSize(size) }

    override fun deleteImages(imageUrls: List<String>) {
        persistenceService.deleteImages(imageUrls)
    }

    // Only meant to be accessed by the ui thread.
    private class Cache<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>(maxSize, 1f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }
}
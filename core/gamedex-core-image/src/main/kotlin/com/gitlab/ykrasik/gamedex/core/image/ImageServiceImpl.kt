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

import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.image.ImageFactory
import com.gitlab.ykrasik.gamedex.util.FileSize
import com.gitlab.ykrasik.gamedex.util.download
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 12/10/2018
 * Time: 10:18
 */
@Singleton
class ImageServiceImpl @Inject constructor(
    private val repo: ImageRepository,
    private val imageFactory: ImageFactory,
    config: ImageConfig
) : ImageService, CoroutineScope {
    override val coroutineContext = Dispatchers.IO

    private val persistedImageCache = Cache<String, Deferred<Image>>(config.fetchCacheSize)
    private val downloadedImageCache = Cache<String, Deferred<Image>>(config.downloadCacheSize)

    // Only meant to be accessed by the ui thread.
    // We disregard the value of persistIfAbsent when we get a cache hit, because
    // if it's already in the cache then it was already called with the exact same 'persistIfAbsent'.
    // A situation where this method is called once with persistIfAbsent=false and again
    // with the same url but persistIfAbsent=true simply doesn't exist.
    override fun fetchImage(url: String, persistIfAbsent: Boolean): Deferred<Image> = persistedImageCache.getOrPut(url) {
        async {
            val persistedBytes = repo[url]
            if (persistedBytes != null) {
                imageFactory(persistedBytes)
            } else {
                val downloadedImage = downloadImage(url).await()
                if (persistIfAbsent) {
                    // Save downloaded image as a fire-and-forget operation.
                    launch {
                        repo[url] = downloadedImage.raw
                    }
                }
                downloadedImage
            }
        }
    }

    override fun downloadImage(url: String): Deferred<Image> = downloadedImageCache.getOrPut(url) {
        async { imageFactory(download(url)) }
    }

    override fun fetchImageSizesExcept(exceptUrls: List<String>): Map<String, FileSize> =
        repo.fetchImageSizesExcept(exceptUrls)

    override fun deleteImages(imageUrls: List<String>) {
        repo.deleteImages(imageUrls)
    }

    // Only meant to be accessed by the ui thread.
    private class Cache<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>(maxSize, 1f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }
}
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

package com.gitlab.ykrasik.gamedex.core.image

import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.image.ImageFactory
import com.gitlab.ykrasik.gamedex.core.storage.Storage
import com.gitlab.ykrasik.gamedex.util.FileSize
import com.gitlab.ykrasik.gamedex.util.download
import com.gitlab.ykrasik.gamedex.util.httpClient
import com.gitlab.ykrasik.gamedex.util.logger
import com.google.inject.BindingAnnotation
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
    @ImageStorage private val storage: Storage<String, ByteArray>,
    private val imageFactory: ImageFactory,
    config: ImageConfig
) : ImageService {
    private val log = logger()

    private val cache = Cache<String, Deferred<Image>>(config.cacheSize)

    override fun createImage(data: ByteArray) = imageFactory(data)

    override suspend fun fetchImage(url: String, persist: Boolean) = withContext(Dispatchers.Main.immediate) {
        try {
            cache.getOrPut(url) {
                GlobalScope.async(Dispatchers.IO) {
                    val bytes = storage[url] ?: run {
                        val downloadedBytes = httpClient.download(url)
                        // Save downloaded image
                        if (persist) {
                            launch {
                                storage[url] = downloadedBytes
                            }
                        }
                        downloadedBytes
                    }
                    createImage(bytes)
                }
            }.await()
        } catch (e: Exception) {
            log.error("Error fetching image $url", e)
            cache -= url
            null
        }
    }

    override fun fetchImageSizesExcept(exceptUrls: List<String>): Map<String, FileSize> {
        val excludedKeys = exceptUrls.toSet()
        return storage.ids().asSequence()
            .filter { it !in excludedKeys }
            .map { it to FileSize(storage.sizeTaken(it)) }
            .toMap()
    }

    override fun deleteImages(imageUrls: List<String>) {
        storage.delete(imageUrls)
    }

    // Not thread-safe.
    private class Cache<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>(maxSize, 1f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }
}

@BindingAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ImageStorage
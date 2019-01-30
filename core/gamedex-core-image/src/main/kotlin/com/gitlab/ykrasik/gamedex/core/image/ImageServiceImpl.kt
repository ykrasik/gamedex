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
import com.gitlab.ykrasik.gamedex.app.api.image.ImageFactory
import com.gitlab.ykrasik.gamedex.core.storage.Storage
import com.gitlab.ykrasik.gamedex.util.FileSize
import com.gitlab.ykrasik.gamedex.util.download
import com.gitlab.ykrasik.gamedex.util.logger
import com.google.inject.BindingAnnotation
import io.ktor.client.HttpClient
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
    @ThumbnailStorage private val thumbnailStorage: Storage<String, ByteArray>,
    @PosterStorage private val posterStorage: Storage<String, ByteArray>,
    private val imageFactory: ImageFactory,
    private val httpClient: HttpClient,
    config: ImageConfig
) : ImageService {
    private val log = logger()

    private val persistedImageCache = Cache<String, Deferred<Image>>(config.fetchCacheSize)
    private val downloadedImageCache = Cache<String, Deferred<Image>>(config.downloadCacheSize)

    override fun createImage(data: ByteArray) = imageFactory(data)

    override suspend fun fetchThumbnail(game: Game) = game.thumbnailUrl?.let { fetchImage(it, thumbnailStorage) }
    override suspend fun fetchPoster(game: Game) = game.posterUrl?.let { fetchImage(it, posterStorage) }

    private suspend fun fetchImage(url: String, storage: Storage<String, ByteArray>): Image? =
        try {
            persistedImageCache.getOrPut(url) {
                GlobalScope.async(Dispatchers.IO) {
                    val persistedBytes = storage[url]
                    if (persistedBytes != null) {
                        createImage(persistedBytes)
                    } else {
                        val downloadedImage = downloadImage0(url)
                        // Save downloaded image
                        launch {
                            storage[url] = downloadedImage.raw
                        }
                        downloadedImage
                    }
                }
            }.await()
        } catch (e: Exception) {
            log.error("Error fetching image $url", e)
            persistedImageCache -= url
            null
        }

    override suspend fun downloadImage(url: String) =
        try {
            downloadImage0(url)
        } catch (e: Exception) {
            log.error("Error downloading image $url", e)
            downloadedImageCache -= url
            null
        }

    private suspend fun downloadImage0(url: String) = downloadedImageCache.getOrPut(url) {
        GlobalScope.async(Dispatchers.IO) {
            val downloadedBytes = httpClient.download(url)
            createImage(downloadedBytes)
        }
    }.await()

    override fun fetchImageSizesExcept(exceptUrls: List<String>): Map<String, FileSize> {
        return listOf(thumbnailStorage, posterStorage)
            .map { it.fetchImageSizesExcept(exceptUrls) }
            .reduce { acc, map -> acc + map }
    }

    private fun Storage<String, ByteArray>.fetchImageSizesExcept(exceptUrls: List<String>): Map<String, FileSize> {
        val excudedKeys = exceptUrls.toSet()
        return ids().asSequence()
            .filter { it !in excudedKeys }
            .map { it to FileSize(sizeTaken(it)) }
            .toMap()
    }

    override fun deleteImages(imageUrls: List<String>) {
        thumbnailStorage.delete(imageUrls)
        posterStorage.delete(imageUrls)
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
annotation class ThumbnailStorage

@BindingAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class PosterStorage
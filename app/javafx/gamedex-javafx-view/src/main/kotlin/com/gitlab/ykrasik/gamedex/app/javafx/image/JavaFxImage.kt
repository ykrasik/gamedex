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

package com.gitlab.ykrasik.gamedex.app.javafx.image

import com.gitlab.ykrasik.gamedex.app.api.image.ImageFactory
import com.gitlab.ykrasik.gamedex.core.image.ImageService
import com.gitlab.ykrasik.gamedex.javafx.control.toImage
import com.gitlab.ykrasik.gamedex.util.getResourceAsByteArray
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.scene.image.Image
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import tornadofx.toProperty
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 11/04/2018
 * Time: 11:10
 */
class JavaFxImage(override val raw: ByteArray, val image: Image) : com.gitlab.ykrasik.gamedex.app.api.image.Image

val com.gitlab.ykrasik.gamedex.app.api.image.Image.image get() = (this as JavaFxImage).image

object JavaFxImageFactory : ImageFactory {
    override fun invoke(data: ByteArray) = JavaFxImage(data, data.toImage())
}

@Singleton
class ImageLoader @Inject constructor(private val imageService: ImageService) {
    private val loading = getResourceAsByteArray("spinner.gif").toImage()
    private val noImage = getResourceAsByteArray("no-image-available.png").toImage()

    // A short-term cache to work around request bursts while the game wall is being first loaded
    private val cache = Cache<String, ObservableValue<Image>>(100)

//    fun loadImage(fetchImageRequests: SendChannel<FetchImageRequest>,
//                  url: String?,
//                  gameId: Int,
//                  persistIfAbsent: Boolean): ObservableValue<Image> {
//        if (url == null) return noImage.toProperty()
//
//        val response = CompletableDeferred<Deferred<com.gitlab.ykrasik.gamedex.app.api.image.Image>>()
//        fetchImageRequests.offer(FetchImageRequest(url, gameId, persistIfAbsent, response))
//
//        val p = SimpleObjectProperty<Image>(loading)
//        launch(Dispatchers.Default) {
//            val image = response.await().await().image
//            withContext(Dispatchers.JavaFx) {
//                p.value = image
//            }
//        }
//        return p
//    }

    fun loadImage(f: () -> CompletableDeferred<Deferred<com.gitlab.ykrasik.gamedex.app.api.image.Image>>?): ObservableValue<Image> {
        val response = f() ?: return noImage.toProperty()
        val p = SimpleObjectProperty<Image>(loading)
        GlobalScope.launch(Dispatchers.IO) {
            val image = response.await().await().image
            withContext(Dispatchers.JavaFx) {
                p.value = image
            }
        }
        return p
    }

    fun loadImage(image: Deferred<com.gitlab.ykrasik.gamedex.app.api.image.Image>?): ObservableValue<Image> =
        image?.toObservableImage() ?: noImage.toProperty()

    // TODO: Delete this
    // Only meant to be accessed by the ui thread.
    fun fetchImage(url: String?, persistIfAbsent: Boolean): ObservableValue<Image> =
        if (url == null) {
            noImage.toProperty()
        } else {
            cache.getOrPut(url) {
                imageService.fetchImage(url, persistIfAbsent).toObservableImage()
            }
        }

    // TODO: Delete this
    // Only meant to be accessed by the ui thread.
    fun downloadImage(url: String?): ObservableValue<Image> =
        if (url == null) {
            noImage.toProperty()
        } else {
            imageService.downloadImage(url).toObservableImage()
        }

    private fun Deferred<com.gitlab.ykrasik.gamedex.app.api.image.Image>.toObservableImage(): ObservableValue<Image> {
        val p = SimpleObjectProperty<Image>(loading)
        if (this.isCompleted) {
            p.value = this.getCompleted().image
        } else {
            GlobalScope.launch(Dispatchers.JavaFx) {
                p.value = await().image
            }
        }
        return p
    }

    // Only meant to be accessed by the ui thread.
    private class Cache<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>(maxSize, 1f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }
}
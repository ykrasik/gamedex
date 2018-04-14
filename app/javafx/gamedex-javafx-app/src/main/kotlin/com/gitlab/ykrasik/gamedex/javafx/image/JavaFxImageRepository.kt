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

package com.gitlab.ykrasik.gamedex.javafx.image

import com.gitlab.ykrasik.gamedex.core.api.image.ImageRepository
import com.gitlab.ykrasik.gamedex.javafx.toImage
import com.gitlab.ykrasik.gamedex.util.getResourceAsByteArray
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.scene.image.Image
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import tornadofx.toProperty
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 11/04/2018
 * Time: 11:10
 */
@Singleton
class JavaFxImageRepository @Inject constructor(private val imageRepository: ImageRepository) {
    private val loading = getResourceAsByteArray("spinner.gif").toImage()
    private val noImage = getResourceAsByteArray("no-image-available.png").toImage().toProperty()

//    private val cache = HashMap<String?, ObservableValue<Image>>(5000)

    // Only meant to be accessed by the ui thread.
    fun fetchImage(url: String?, gameId: Int, persistIfAbsent: Boolean): ObservableValue<Image> =
        if (url == null) {
            noImage
        } else {
//            cache.getOrPut(url) {
                imageRepository.fetchImage(url, gameId, persistIfAbsent).toObservableImage()
//            }
        }

    // Only meant to be accessed by the ui thread.
    fun downloadImage(url: String?): ObservableValue<Image> =
        if (url == null) {
            noImage
        } else {
            imageRepository.downloadImage(url).toObservableImage()
        }

    private fun Deferred<ByteArray>.toObservableImage(): ObservableValue<Image> {
        val p = SimpleObjectProperty<Image>(loading)
        launch(CommonPool) {
            val image = await().toImage()
            withContext(JavaFx) {
                p.value = image
            }
        }
        return p
    }
}
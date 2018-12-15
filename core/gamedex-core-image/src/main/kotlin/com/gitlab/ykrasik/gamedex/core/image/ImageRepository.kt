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

import com.gitlab.ykrasik.gamedex.core.storage.Storage
import com.gitlab.ykrasik.gamedex.util.FileSize
import com.google.inject.BindingAnnotation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 21/09/2018
 * Time: 22:14
 */
@Singleton
class ImageRepository @Inject constructor(@ImageStorage private val storage: Storage<String, ByteArray>) {
    operator fun set(url: String, data: ByteArray) {
        storage[url] = data
    }

    operator fun get(url: String): ByteArray? = storage[url]

    fun fetchImageSizesExcept(exceptUrls: List<String>): Map<String, FileSize> {
        val excudedKeys = exceptUrls.toSet()
        return storage.ids()
            .asSequence()
            .filter { it !in excudedKeys }
            .map { it to FileSize(storage.sizeTaken(it)) }
            .toMap()
    }

    fun deleteImages(imageUrls: List<String>) = imageUrls.forEach { storage.delete(it) }
}

@BindingAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ImageStorage
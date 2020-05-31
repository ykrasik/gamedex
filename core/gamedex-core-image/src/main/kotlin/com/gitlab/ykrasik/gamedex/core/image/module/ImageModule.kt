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

package com.gitlab.ykrasik.gamedex.core.image.module

import com.gitlab.ykrasik.gamedex.core.image.ImageConfig
import com.gitlab.ykrasik.gamedex.core.image.ImageService
import com.gitlab.ykrasik.gamedex.core.image.ImageServiceImpl
import com.gitlab.ykrasik.gamedex.core.image.ImageStorage
import com.gitlab.ykrasik.gamedex.core.image.presenter.ImageGalleryPresenter
import com.gitlab.ykrasik.gamedex.core.image.presenter.ShowImageGalleryPresenter
import com.gitlab.ykrasik.gamedex.core.module.InternalCoreModule
import com.gitlab.ykrasik.gamedex.core.storage.FileStorage
import com.gitlab.ykrasik.gamedex.core.storage.Storage
import com.gitlab.ykrasik.gamedex.util.base64Decoded
import com.gitlab.ykrasik.gamedex.util.base64Encoded
import com.gitlab.ykrasik.gamedex.util.filePath
import com.gitlab.ykrasik.gamedex.util.toUrl
import com.google.inject.Provides
import com.typesafe.config.Config
import io.github.config4k.extract
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 12/10/2018
 * Time: 10:24
 */
object ImageModule : InternalCoreModule() {
    val configFile = "/com/gitlab/ykrasik/gamedex/core/image/image.conf"

    override fun configure() {
        bind(ImageService::class.java).to(ImageServiceImpl::class.java)

        bindPresenter(ShowImageGalleryPresenter::class)
        bindPresenter(ImageGalleryPresenter::class)
    }

    @Provides
    @Singleton
    fun imageConfig(config: Config): ImageConfig = config.extract("gameDex.image")

    @Provides
    @Singleton
    @ImageStorage
    fun imageStorage(): Storage<String, ByteArray> = FileStorage.binary("cache/images").stringId(
        keyTransform = { url ->
            val extension = url.toUrl().filePath.extension
            val indexOfFirstNonWord = extension.indexOfFirst { !it.isLetterOrDigit() }
            val sanitizedExtension = if (indexOfFirstNonWord > 0) extension.substring(0, indexOfFirstNonWord) else extension
            "${url.base64Encoded()}.$sanitizedExtension"
        },
        reverseKeyTransform = { fileName -> fileName.substringBeforeLast(".").base64Decoded() }
    )
}
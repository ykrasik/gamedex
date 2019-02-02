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

package com.gitlab.ykrasik.gamedex.app.javafx.common

import com.gitlab.ykrasik.gamedex.FileTree
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.common.ViewCommonOps
import com.gitlab.ykrasik.gamedex.app.javafx.image.DomainImage
import com.gitlab.ykrasik.gamedex.app.javafx.image.JavaFxImage
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.javafx.control.toImage
import com.gitlab.ykrasik.gamedex.provider.GameProviderMetadata
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.Ref
import com.gitlab.ykrasik.gamedex.util.getResourceAsByteArray
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import tornadofx.toProperty
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 19/01/2019
 * Time: 22:33
 */
@Singleton
class JavaFxCommonOps @Inject constructor(private val ops: ViewCommonOps) {
    private val loading = getResourceAsByteArray("spinner.gif").toImage()
    private val noImage = getResourceAsByteArray("no-image-available.png").toImage()

    fun fetchThumbnail(game: Game?): ObservableValue<JavaFxImage> = game.ifNotNull {
        loadImage {
            ops.fetchThumbnail(it)
        }
    }

    fun fetchPoster(game: Game?): ObservableValue<JavaFxImage> = game.ifNotNull {
        loadImage {
            ops.fetchPoster(it)
        }
    }

    fun downloadImage(url: String?): ObservableValue<JavaFxImage> = url.ifNotNull {
        loadImage {
            ops.downloadImage(it)
        }
    }

    private inline fun <T> T?.ifNotNull(f: (T) -> ObservableValue<JavaFxImage>): ObservableValue<JavaFxImage> =
        if (this != null) {
            f(this)
        } else {
            noImage.toProperty()
        }

    private inline fun loadImage(crossinline f: suspend () -> DomainImage?): ObservableValue<JavaFxImage> {
        val p = SimpleObjectProperty(loading)
        GlobalScope.launch(Dispatchers.JavaFx, CoroutineStart.UNDISPATCHED) {
            val image = f()
            p.value = image?.image ?: noImage
        }
        return p
    }

    fun fetchFileTree(game: Game): Ref<FileTree> = ops.fetchFileTree(game)

    val providers: List<GameProviderMetadata> = ops.providers
    val providerLogos: Map<ProviderId, JavaFxImage> = ops.providerLogos.mapValues { it.value.image }.withDefault { noImage }
    fun providerLogo(providerId: ProviderId): JavaFxImage = providerLogos.getValue(providerId)

    fun youTubeGameplayUrl(game: Game): String = ops.youTubeGameplayUrl(game)
}
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

package com.gitlab.ykrasik.gamedex.app.javafx.common

import com.gitlab.ykrasik.gamedex.FileTree
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.Version
import com.gitlab.ykrasik.gamedex.app.api.common.ViewCommonOps
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.util.AsyncValueState
import com.gitlab.ykrasik.gamedex.app.javafx.image.DomainImage
import com.gitlab.ykrasik.gamedex.app.javafx.image.JavaFxImage
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.javafx.JavaFxScope
import com.gitlab.ykrasik.gamedex.javafx.control.toImage
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.getResourceAsByteArray
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

    val applicationVersion: Version = ops.applicationVersion

    fun fetchThumbnail(game: Game?): ObservableValue<JavaFxImage> = fetchImage(game?.thumbnailUrl, game, persist = true, name = "fetchThumbnail")

    fun fetchPoster(game: Game?): ObservableValue<JavaFxImage> = fetchImage(game?.posterUrl, game, persist = true, name = "fetchPoster")

    fun fetchScreenshot(game: Game, url: String): ObservableValue<JavaFxImage> = fetchImage(url, game, persist = true, name = "fetchScreenshot")

    fun fetchImage(url: String?, persist: Boolean): ObservableValue<JavaFxImage> = fetchImage(url, null, persist, name = "fetchImage")

    private fun fetchImage(url: String?, game: Game?, persist: Boolean, name: String): ObservableValue<JavaFxImage> = url.ifNotNull {
        val property = SimpleObjectProperty(loading)
        JavaFxScope.launch(CoroutineName("$game.$name"), start = CoroutineStart.UNDISPATCHED) {
            val result = ops.fetchImage(it, persist).first { it != AsyncValueState.loading<Image>() }
            when (result) {
                is AsyncValueState.Result -> property.value = result.result.image
                is AsyncValueState.Error -> property.value = noImage
                else -> Unit
            }
        }
        property
    }

    private inline fun <T> T?.ifNotNull(f: (T) -> ObservableValue<JavaFxImage>): ObservableValue<JavaFxImage> =
        if (this != null) {
            f(this)
        } else {
            noImage.toProperty()
        }

    private inline fun loadImage(crossinline f: suspend () -> DomainImage?): ObservableValue<JavaFxImage> {
        val p = SimpleObjectProperty(loading)
        JavaFxScope.launch {
            val image = f()
            p.value = image?.image ?: noImage
        }
        return p
    }

    fun fetchFileTree(game: Game): StateFlow<FileTree?> = ops.fetchFileTree(game)

    val providers: List<GameProvider.Metadata> = ops.providers
    val providerLogos: Map<ProviderId, JavaFxImage> = ops.providerLogos.mapValues { it.value.image }.withDefault { noImage }
    fun providerLogo(providerId: ProviderId): JavaFxImage = providerLogos.getValue(providerId)

    fun youTubeGameplayUrl(name: String, platform: Platform): String = ops.youTubeGameplayUrl(name, platform)
    fun youTubeGameplayUrl(game: Game): String = youTubeGameplayUrl(game.name, game.platform)
}

private val noImage = JavaFxCommonOps::class.java.getResource("no-image-available.png").readBytes().toImage()

val JavaFxImage.isNoImage: Boolean get() = this === noImage
val ObservableValue<JavaFxImage>.isNoImage: Boolean get() = value.isNoImage
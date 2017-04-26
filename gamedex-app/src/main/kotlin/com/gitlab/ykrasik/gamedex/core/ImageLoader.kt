package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.ui.UIResources
import com.gitlab.ykrasik.gamedex.ui.toImage
import com.gitlab.ykrasik.gamedex.util.download
import com.gitlab.ykrasik.gamedex.util.logger
import com.google.inject.Inject
import com.google.inject.Singleton
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.run

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 18:30
 */
// TODO: See if image caching can be moved into the ImageLoader - Guava LRU cache?
@Singleton
open class ImageLoader @Inject constructor(private val persistenceService: PersistenceService) {
    private val log by logger()

    private val notAvailable = SimpleObjectProperty(UIResources.Images.notAvailable)

    open fun fetchImage(gameId: Int, url: String?, saveIfAbsent: Boolean): ReadOnlyObjectProperty<Image> {
        if (url == null) return notAvailable
        return loadImage {
            fetchOrDownloadImage(gameId, url, saveIfAbsent)
        }
    }

    open fun downloadImage(url: String?): ReadOnlyObjectProperty<Image> {
        if (url == null) return notAvailable
        return loadImage {
            downloadImage(url)
        }
    }

    private fun loadImage(f: suspend () -> ByteArray): ObjectProperty<Image> {
        val imageProperty = SimpleObjectProperty<Image>(UIResources.Images.loading)
        launch(CommonPool) {
            val image = f().toImage()
            run(JavaFx) {
                imageProperty.value = image
            }
        }
        return imageProperty
    }

    private suspend fun fetchOrDownloadImage(gameId: Int, url: String, saveIfAbsent: Boolean): ByteArray {
        val storedImage = persistenceService.fetchImage(url)
        if (storedImage != null) return storedImage

        val downloadedImage = downloadImage(url)
        if (saveIfAbsent) {
            launch(CommonPool) {
                // Save downloaded image asynchronously
                persistenceService.insertImage(gameId, url, downloadedImage)
            }
        }
        return downloadedImage
    }

    private suspend fun downloadImage(url: String): ByteArray = run(CommonPool) {
        log.info { "Downloading: $url..." }
        val bytes = download(url)
        log.info { "Done. Size: ${bytes.size}" }
        bytes
    }
}
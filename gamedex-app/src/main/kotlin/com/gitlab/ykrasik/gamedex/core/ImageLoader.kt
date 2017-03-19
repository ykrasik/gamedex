package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.common.util.download
import com.gitlab.ykrasik.gamedex.common.util.logger
import com.gitlab.ykrasik.gamedex.common.util.toImage
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.ui.UIResources
import com.google.inject.Inject
import com.google.inject.Singleton
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlin.coroutines.experimental.CoroutineContext

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 18:30
 */
@Singleton
class ImageLoader @Inject constructor(private val persistenceService: PersistenceService) {
    private val log by logger()

    private val downloadContext = newFixedThreadPoolContext(1, "image-downloader")
    private val fetchContext = newFixedThreadPoolContext(1, "image-fetcher")

    private val notAvailable = SimpleObjectProperty(UIResources.Images.notAvailable)

    fun fetchImage(id: Int?): ReadOnlyObjectProperty<Image> {
        if (id == null) return notAvailable
        return loadImage(fetchContext) {
            fetchOrDownloadImage(id)
        }
    }

    fun downloadImage(url: String?): ReadOnlyObjectProperty<Image> {
        if (url == null) return notAvailable
        return loadImage(downloadContext) {
            downloadImage(url)
        }
    }

    private fun loadImage(context: CoroutineContext, f: suspend () -> ByteArray): ObjectProperty<Image> {
        val imageProperty = SimpleObjectProperty<Image>()
        async(JavaFx) {
            imageProperty.value = UIResources.Images.loading
        }
        launch(context) {
            val image = f().toImage()
            run(JavaFx) {
                imageProperty.value = image
            }
        }
        return imageProperty
    }

    private suspend fun fetchOrDownloadImage(id: Int): ByteArray {
        val image = persistenceService.fetchImage(id)
        image.bytes?.let { return it  }

        val downloadedImage = downloadImage(image.url)
        async(CommonPool) {
            // Save downloaded image in a different thread.
            persistenceService.updateImage(image.copy(bytes = downloadedImage))
        }
        return downloadedImage
    }

    private suspend fun downloadImage(url: String): ByteArray = run(downloadContext) {
        log.info { "Downloading: $url..." }
        val bytes = download(url, stream = true) { _, _ -> }
        log.info { "Done. Size: ${bytes.size}" }
        bytes
    }
}
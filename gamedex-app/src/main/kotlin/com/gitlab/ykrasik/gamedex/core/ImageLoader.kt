package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.common.util.download
import com.gitlab.ykrasik.gamedex.common.util.logger
import com.gitlab.ykrasik.gamedex.common.util.toImage
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.ui.UIResources
import com.gitlab.ykrasik.gamedex.util.NotifiableJob
import com.gitlab.ykrasik.gamedex.util.notifiableJob
import com.google.inject.Inject
import com.google.inject.Singleton
import javafx.animation.FadeTransition
import javafx.scene.image.ImageView
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.javafx.JavaFx
import tornadofx.fade
import tornadofx.seconds

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 18:30
 */
@Singleton
class ImageLoader @Inject constructor(private val persistenceService: PersistenceService) {
    private val log by logger()

    // TODO: More threads? Split into 2 contexts - fetch & download?
    private val context = newSingleThreadContext("image-loader")

    private val downloadJobCache = mutableMapOf<String, NotifiableJob<Job>>()
    private val fetchJobCache = mutableMapOf<Int, NotifiableJob<Job>>()

    private val fadeInDuration = 0.02.seconds
    private val fadeOutDuration = 0.05.seconds

    fun downloadImage(url: String, into: ImageView): NotifiableJob<Job> = downloadJobCache.getOrPut(url) {
        createJob { notification ->
            downloadImage(url, into, notification)
        }
    }

    fun fetchImage(id: Int, into: ImageView): NotifiableJob<Job> = fetchJobCache.getOrPut(id) {
        createJob { notification ->
            fetchImage(id, into, notification)
        }
    }

    private inline fun createJob(crossinline f: (Notification) -> Unit): NotifiableJob<Job> =
        notifiableJob { notification ->
            launch(context) {
                f(notification)
            }
        }

    private fun downloadImage(url: String, imageView: ImageView, notification: Notification): ByteArray {
        return loadImage(imageView) {
            // FIXME: Create a rest client api for unit tests.
            log.info { "Downloading: $url..." }
            val bytes = download(url, stream = true) { downloaded, total ->
                notification.progress(downloaded, total)
            }
            log.info { "Done. Size: ${bytes.size}" }
            bytes
        }
    }

    private fun fetchImage(id: Int, imageView: ImageView, notification: Notification) {
        loadImage(imageView) {
            val image = persistenceService.fetchImage(id)
            image.bytes?.let { return@loadImage it  }

            log.debug { "Image $id does not exist in db." }
            val bytes = downloadImage(image.url, imageView, notification)

            async(CommonPool) {
                // Save downloaded image in a different thread.
                persistenceService.updateImage(image.copy(bytes = bytes))
            }
            bytes
        }
    }

    private inline fun loadImage(imageView: ImageView, crossinline f: () -> ByteArray): ByteArray {
        async(JavaFx) {
            imageView.image = UIResources.Images.loading
        }

        val bytes = f()
        val image = bytes.toImage()

        val fadeTransition = FadeTransition(fadeOutDuration, imageView)
        fadeTransition.fromValue = 1.0
        fadeTransition.toValue = 0.0
        fadeTransition.setOnFinished {
            imageView.image = image
            imageView.fade(fadeInDuration, 1.0, play = true) {
                fromValue = 0.0
            }
        }
        async(JavaFx) {
            fadeTransition.play()
        }

        return bytes
    }
}
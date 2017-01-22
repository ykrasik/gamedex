package com.gitlab.ykrasik.gamedex.core

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.common.toImage
import com.github.ykrasik.gamedex.datamodel.GameImage
import com.github.ykrasik.gamedex.datamodel.GameImageId
import com.gitlab.ykrasik.gamedex.core.ui.UIResources
import com.gitlab.ykrasik.gamedex.core.util.GamedexTask
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.Inject
import com.google.inject.Singleton
import javafx.animation.FadeTransition
import javafx.beans.property.SimpleObjectProperty
import javafx.concurrent.Task
import javafx.scene.image.ImageView
import tornadofx.*
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 18:30
 */
@Singleton
class ImageLoader @Inject constructor(
    private val persistenceService: PersistenceService
) {
    private val log by logger()

    // TODO: More threads? Split into 2 executors - fetch & download?
    private val executorService = MoreExecutors.getExitingExecutorService(Executors.newFixedThreadPool(
        1, ThreadFactoryBuilder().setNameFormat("image-fetcher-%d").build()
    ) as ThreadPoolExecutor)

    private val downloadTaskCache = mutableMapOf<String, DownloadImageTask>()
    private val fetchTaskCache = mutableMapOf<GameImageId, FetchImageTask>()

    private val fadeInDuration = 0.02.seconds
    private val fadeOutDuration = 0.05.seconds

    fun loadUrl(url: String, into: ImageView): Task<*> = submitTask(downloadImageTask(url, into))
    fun loadImage(id: GameImageId, into: ImageView): Task<*> = submitTask(fetchImageTask(id, into))

    private fun downloadImageTask(url: String, imageView: ImageView?): DownloadImageTask =
        createTask(downloadTaskCache, url, imageView) { DownloadImageTask(url) }

    private fun fetchImageTask(id: GameImageId, imageView: ImageView): FetchImageTask =
        createTask(fetchTaskCache, id, imageView) { FetchImageTask(id) }

    private fun <K, T : ImageTask> createTask(cache: MutableMap<K, T>,
                                              key: K,
                                              imageView: ImageView?,
                                              ctor: () -> T): T = synchronized(cache) {
        val task = cache[key]?.apply {
            log.debug { "Task already in progress: $this" }
        } ?: ctor().apply {
            log.debug { "Created new task: $this" }
            cache[key] = this
            this.onSucceeded {
                log.debug { "Removing completed task: ${this@apply}" }
                cache.remove(key)
            }
        }
        imageView?.let {
            task.imageView = it
        }
        task
    }

    private fun submitTask(task: GamedexTask<ByteArray>): Task<ByteArray> = task.apply {
        executorService.execute(this)
    }

    private inner abstract class ImageTask() : GamedexTask<ByteArray>(log) {
        val imageViewProperty = SimpleObjectProperty<ImageView>()
        var imageView: ImageView? by imageViewProperty

        init {
            imageViewProperty.addListener { value, old, new ->
                if (isCompleted) {
                    // For edge cases where the task is completed, but still in the cache.
                    setImage()
                } else if (!isStopped && old != new) {
                    setLoading()
                }
            }
        }

        override fun succeeded() {
            setImage()
        }

        private fun setLoading() {
            imageView?.image = UIResources.Images.loading
        }

        private fun setImage() {
            val transition = createTransition()
            transition?.play()
        }

        private fun createTransition(): FadeTransition? = imageView?.let { imageView ->
            val fadeTransition = FadeTransition(fadeOutDuration, imageView)
            fadeTransition.fromValue = 1.0
            fadeTransition.toValue = 0.0
            fadeTransition.setOnFinished {
                imageView.image = value.toImage()
                imageView.fade(fadeInDuration, 1.0, play = true) {
                    fromValue = 0.0
                }
            }
            return fadeTransition
        }
    }

    private inner class DownloadImageTask(private val url: String) : ImageTask() {
        override fun call(): ByteArray {
            log.info { "Downloading: $url..." }

            // FIXME: Create a rest client api. Or use TornadoFX.
            val (request, response, result) = Fuel.download(url)
                .header("User-Agent" to "Kotlin-Fuel")
                .destination { response, url -> File.createTempFile("temp", ".tmp") }
                .progress { readBytes, totalBytes -> updateProgress(readBytes, totalBytes) }
                .response()

            val bytes = when (result) {
                is Result.Failure -> throw result.error
                is Result.Success -> result.value
            }
            log.info { "Done. Size: ${bytes.size}" }
            return bytes
        }

        override fun toString() = "DownloadImageTask($url)"
    }

    private inner class FetchImageTask(private val id: GameImageId) : ImageTask() {
        override fun call(): ByteArray {
            val image = persistenceService.fetchImage(id)
            if (image.bytes != null) {
                return image.bytes!!
            }

            // TODO: If download task fails, call updater with null url to remove it.
            log.debug { "Image $id does not exist in db." }
            val url = checkNotNull(image.url) { "${image.id} has no url to download an image from!" }
            val bytes = downloadImage(url)
            executorService.execute {
                // Save downloaded image in a different thread.
                persistenceService.updateImage(GameImage(id, url, bytes))
            }
            return bytes
        }

        private fun downloadImage(url: String): ByteArray {
            // TODO: Calling with imageView = null to avoid it playing the transition when done download.
            // TODO: Yes, this is a dirty hack.
            val downloadTask = downloadImageTask(url, imageView = null)
            downloadTask.progressProperty().onChange {
                // TODO: Test the progress property.
                updateProgress(it, 1.0)
            }
            downloadTask.run()
            return downloadTask.get()
        }

        override fun toString() = "FetchImageTask($id)"
    }
}
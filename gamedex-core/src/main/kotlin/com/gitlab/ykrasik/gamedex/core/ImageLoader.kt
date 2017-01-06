package com.gitlab.ykrasik.gamedex.core

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.common.toImage
import com.github.ykrasik.gamedex.datamodel.GameImage
import com.github.ykrasik.gamedex.datamodel.GameImageId
import com.gitlab.ykrasik.gamedex.core.ui.GamedexTask
import com.gitlab.ykrasik.gamedex.core.ui.UIResources
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.Inject
import com.google.inject.Singleton
import javafx.animation.FadeTransition
import javafx.application.Platform.runLater
import javafx.concurrent.Task
import javafx.scene.image.ImageView
import tornadofx.fade
import tornadofx.onChange
import tornadofx.seconds
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 18:30
 */
// FIXME: Have a look at JavaFX Service.
@Singleton
class ImageLoader @Inject constructor(
    private val persistenceService: PersistenceService
) {
    private val log by logger()

    // TODO: More threads?
    private val executorService: ExecutorService = MoreExecutors.getExitingExecutorService(Executors.newFixedThreadPool(
        1, ThreadFactoryBuilder().setNameFormat("image-fetcher-%d").build()
    ) as ThreadPoolExecutor)

    private val downloadTaskCache = mutableMapOf<String, DownloadImageTask>()
    private val fetchTaskCache = mutableMapOf<GameImageId, FetchImageTask>()

    private val fadeInDuration = 0.02.seconds
    private val fadeOutDuration = 0.05.seconds

    fun loadUrl(url: String, into: ImageView): Task<*> =
        submitTask(newDownloadImageTask(url), into)

    fun loadImage(id: GameImageId, into: ImageView): Task<*> =
        submitTask(newFetchImageTask(id), into)

    private fun newDownloadImageTask(url: String): DownloadImageTask = createTask(downloadTaskCache, url) {
        DownloadImageTask(url)
    }

    private fun newFetchImageTask(id: GameImageId): FetchImageTask = createTask(fetchTaskCache, id) {
        FetchImageTask(id)
    }

    private fun <K, T> createTask(cache: MutableMap<K, T>, key: K, ctor: () -> T): T = synchronized(cache) {
        cache[key]?.let {
            log.debug { "Task already in progress: $it" }
            return it
        }
        return ctor().apply {
            log.debug { "Created new task: $this" }
            cache[key] = this
        }
    }

    private fun submitTask(task: Task<ByteArray>, into: ImageView): Task<*> {
        // Set imageView to a "loading" image.
        into.image = UIResources.Images.loading

        val transition = createTransition(task, into)
        task.setOnSucceeded {
            runLater {
                transition.play()
            }
        }

        executorService.execute(task)
        return task
    }

    private fun createTransition(task: Task<ByteArray>, into: ImageView): FadeTransition {
        val fadeTransition = FadeTransition(fadeOutDuration, into)
        fadeTransition.fromValue = 1.0
        fadeTransition.toValue = 0.0
        fadeTransition.setOnFinished {
            into.image = task.value.toImage()
            into.fade(fadeInDuration, 1.0, play = true) {
                fromValue = 0.0
            }
        }
        return fadeTransition
    }

    private inner abstract class CachedImageTask<Task, K>(
        private val cache: MutableMap<K, Task>,
        private val cacheKey: K
    ) : GamedexTask<ByteArray>(log) {

        override fun succeeded() { cache.remove(cacheKey) }
        override fun failed() = succeeded()
        override fun cancelled() = succeeded()
    }

    private inner class DownloadImageTask(
        private val url: String
    ) : CachedImageTask<DownloadImageTask, String>(downloadTaskCache, url) {

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

    private inner class FetchImageTask(
        private val id: GameImageId
    ) : CachedImageTask<FetchImageTask, GameImageId>(fetchTaskCache, id) {

        override fun call(): ByteArray {
            val image = persistenceService.images.fetchImage(id)
            if (image.bytes != null) {
                return image.bytes!!
            }

            // TODO: If download task fails, call updater with null url to remove it.
            val url = checkNotNull(image.url) { "${image.id} has no url to download an image from!" }
            val bytes = downloadImage(url)
            executorService.execute {
                // Save downloaded image in a different thread.
                persistenceService.images.updateImage(GameImage(id, url, bytes))
            }
            return bytes
        }

        private fun downloadImage(url: String): ByteArray {
            val downloadTask = newDownloadImageTask(url)
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
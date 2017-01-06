package com.gitlab.ykrasik.gamedex.core

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.common.toImage
import com.github.ykrasik.gamedex.datamodel.ImageData
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

    private val fadeInDuration = 0.02.seconds
    private val fadeOutDuration = 0.05.seconds

    private val downloadTaskCache = mutableMapOf<String, Task<ByteArray>>()

    fun loadUrl(url: String, into: ImageView): Task<*> {
        return submitTask(into) {
            createDownloadImageTask(url)
        }
    }

    fun loadThumbnail(gameId: Int, into: ImageView): Task<*> {
        return submitTask(into) {
            object : GamedexTask<ByteArray>(log) {
                override fun call(): ByteArray {
                    val imageData = persistenceService.images.fetchThumbnail(gameId)
                    val bytes = if (imageData.bytes != null) {
                        imageData.bytes!!
                    } else {
                        val url = checkNotNull(imageData.url) { "No thumbnail saved, and no url to download it from!" }
                        val downloadTask = createDownloadImageTask(url)
                        downloadTask.progressProperty().onChange {
//                            log.info { "Progress: $it" }
                            // TODO: Test the progress property.
                            updateProgress(it, 1.0)
                        }
                        downloadTask.run()
                        val bytes = downloadTask.get()
                        executorService.execute {
                            persistenceService.images.updateThumbnail(gameId, ImageData(bytes, url))
                        }
                        bytes
                    }
                    return bytes
                }
            }
        }
    }

    fun loadPoster(gameId: Int, into: ImageView): Task<*> {
        TODO("loadPoster")
    }

    private fun submitTask(into: ImageView, taskFactory: () -> Task<ByteArray>): Task<*> {
        val task = taskFactory()

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

    private fun createDownloadImageTask(url: String): Task<ByteArray> = synchronized(downloadTaskCache) {
        downloadTaskCache[url]?.let {
            log.debug { "Download already in progress: $url" }
            return it
        }
        return DownloadImageTask(url).apply {
            downloadTaskCache[url] = this
        }
    }

    private inner class DownloadImageTask(private val url: String) : GamedexTask<ByteArray>(log) {
        override fun call(): ByteArray {
            log.info { "Downloading: $url..." }

            // FIXME: Create a rest client api. Or use TornadoFX.
            val (request, response, result) = Fuel.download(url)
                .header("User-Agent" to "Kotlin-Fuel")
                .destination { response, url -> File.createTempFile("temp", ".tmp") }
                .progress { readBytes, totalBytes -> updateProgress(readBytes, totalBytes) }
                .response()

            val imageData = when (result) {
                is Result.Failure -> throw result.error
                is Result.Success -> result.value
            }
            log.info { "Done. Size: ${imageData.size}" }
            return imageData
        }

        override fun succeeded() {
            downloadTaskCache.remove(url)
        }
        override fun failed() = succeeded()
        override fun cancelled() = succeeded()
    }
}
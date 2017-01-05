package com.gitlab.ykrasik.gamedex.core

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.common.toImage
import com.github.ykrasik.gamedex.datamodel.ImageData
import com.gitlab.ykrasik.gamedex.core.ui.GamedexTask
import com.gitlab.ykrasik.gamedex.core.ui.UIResources
import com.gitlab.ykrasik.gamedex.persistence.dao.ImageDao
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.Inject
import com.google.inject.Singleton
import javafx.animation.FadeTransition
import javafx.application.Platform.runLater
import javafx.concurrent.Task
import javafx.scene.image.ImageView
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
    private val dao: ImageDao
) {
    private val log by logger()

    // TODO: More threads?
    private val executorService: ExecutorService = MoreExecutors.getExitingExecutorService(Executors.newFixedThreadPool(
        1, ThreadFactoryBuilder().setNameFormat("image-fetcher-%d").build()
    ) as ThreadPoolExecutor)

    private val fadeInDuration = 0.02.seconds
    private val fadeOutDuration = 0.05.seconds

    fun loadUrl(url: String, into: ImageView): Task<*> {
        return submitTask(into) {
            createDownloadImageTask(url)
        }
    }

    fun loadThumbnail(gameId: Int, into: ImageView): Task<*> {
        return submitTask(into) {
            object : GamedexTask<ByteArray>(log) {
                override fun call(): ByteArray {
                    val imageData = dao.fetchThumbnail(gameId)
                    val bytes = if (imageData.bytes != null) {
                        imageData.bytes!!
                    } else {
                        val url = checkNotNull(imageData.url) { "No thumbnail saved, and no url to download it from!" }
                        val downloadTask = createDownloadImageTask(url)
                        downloadTask.progressProperty().onChange {
                            log.info { "Progress: $it" }
                            updateProgress(it, 1.0)
                        }
                        downloadTask.run()
                        val bytes = downloadTask.get()
                        executorService.execute {
                            dao.updateThumbnail(gameId, ImageData(bytes, url))
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
        runLater {
            into.image = UIResources.Images.loading
        }

        prepareTransition(task, into)

        executorService.execute(task)
        return task
    }

    private fun prepareTransition(task: Task<ByteArray>, into: ImageView) {
        val fadeIn = FadeTransition(fadeInDuration, into)
        fadeIn.fromValue = 0.0
        fadeIn.toValue = 1.0

        val fadeOut = FadeTransition(fadeOutDuration, into)
        fadeOut.fromValue = 1.0
        fadeOut.toValue = 0.0
        fadeOut.setOnFinished {
            into.image = task.value.toImage()
            fadeIn.play()
        }

        task.setOnSucceeded {
            runLater {
                fadeOut.play()
            }
        }
    }

    private fun createDownloadImageTask(url: String): Task<ByteArray> {
        // FIXME: Create a rest client api. Or use TornadoFX.
        return object : GamedexTask<ByteArray>(log) {
            override fun call(): ByteArray {
                log.info { "Downloading: $url..." }
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
        }
    }
}
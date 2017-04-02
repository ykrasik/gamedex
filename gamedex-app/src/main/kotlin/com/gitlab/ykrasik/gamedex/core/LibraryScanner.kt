package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.common.TimeProvider
import com.gitlab.ykrasik.gamedex.common.datamodel.Library
import com.gitlab.ykrasik.gamedex.common.datamodel.MetaData
import com.gitlab.ykrasik.gamedex.common.util.collapseSpaces
import com.gitlab.ykrasik.gamedex.common.util.emptyToNull
import com.gitlab.ykrasik.gamedex.persistence.AddGameRequest
import com.gitlab.ykrasik.gamedex.provider.DataProviderService
import com.gitlab.ykrasik.gamedex.util.NotifiableJob
import com.gitlab.ykrasik.gamedex.util.notifiableJob
import kotlinx.coroutines.experimental.channels.ProducerJob
import kotlinx.coroutines.experimental.channels.produce
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.CoroutineContext

/**
 * User: ykrasik
 * Date: 12/10/2016
 * Time: 12:52
 */
@Singleton
class LibraryScanner @Inject constructor(
    private val providerService: DataProviderService,
    private val timeProvider: TimeProvider
) {
    private val metaDataRegex = "(\\[.*?\\])|(-)".toRegex()

    fun refresh(library: Library,
                excludedPaths: Set<File>,
                context: CoroutineContext): NotifiableJob<ProducerJob<AddGameRequest>> = notifiableJob { notification ->
        notification.message = "Refreshing library: '$library'..."
        val newPaths = PathDetector(excludedPaths).detectNewPaths(library.path)

        produce<AddGameRequest>(context) {
            val newGames = newPaths.mapIndexedNotNull { i, path ->
                if (!isActive) return@mapIndexedNotNull

                notification.progress(i, newPaths.size)
                val addGameRequest = processPath(path, library)
                addGameRequest?.let { send(it) }
            }
            notification.message = "Done refreshing library: '$library'. Added ${newGames.size} new games."
            channel.close()
        }
    }

    private suspend fun processPath(path: File, library: Library): AddGameRequest? {
        val name = path.normalizeName().emptyToNull() ?: return null
        val platform = library.platform

        val providerGame = providerService.fetch(name, platform, path) ?: return null

        return AddGameRequest(
            metaData = MetaData(library.id, path, lastModified = timeProvider.now()),
            gameData = providerGame.gameData,
            providerData = providerGame.providerData,
            imageUrls = providerGame.imageUrls
        )
    }

    // Remove all metaData enclosed with '[]' from the file name and collapse all spaces into a single space.
    private fun File.normalizeName(): String = name.replace(metaDataRegex, "").collapseSpaces()
}
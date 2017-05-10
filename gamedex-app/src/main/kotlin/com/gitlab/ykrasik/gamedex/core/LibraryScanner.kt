package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.MetaData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.repository.AddGameRequest
import com.gitlab.ykrasik.gamedex.util.logger
import kotlinx.coroutines.experimental.channels.produce
import org.joda.time.DateTime
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
// TODO: Consider changing the structure, having a NewGameDetector that returns a stream of MetaData objects instead.
// TODO: This will allow to move the notification logic outside the class, if this is what I want.
// TODO: This class doesn't do much
class LibraryScanner @Inject constructor(
    private val newDirectoryDetector: NewDirectoryDetector,
    private val providerService: GameProviderService
) {

    private val log = logger()

    fun scan(context: CoroutineContext, libraries: List<Library>, games: List<Game>, progress: TaskProgress) = produce<AddGameRequest>(context) {
        progress.message = "Scanning for new directories..."

        val excludedDirectories = libraries.map(Library::path).toSet() + games.map(Game::path).toSet()
        val newDirectories = libraries.flatMap { library ->
            val paths = if (library.platform != Platform.excluded) {
                log.debug("Scanning library: $library")
                newDirectoryDetector.detectNewDirectories(library.path, excludedDirectories - library.path)
            } else {
                emptyList()
            }
            paths.map { library to it }
        }

        progress.message = "Scanning for new directories: ${newDirectories.size} new directories."

        newDirectories.forEachIndexed { i, (library, directory) ->
            if (!isActive) return@forEachIndexed

            progress.progress(i, newDirectories.size - 1)
            val addGameRequest = processDirectory(directory, library, progress)
            addGameRequest?.let { send(it) }
        }
        channel.close()
    }

    private suspend fun processDirectory(directory: File, library: Library, progress: TaskProgress): AddGameRequest? {
        val providerData = providerService.search(directory.name, library.platform, directory, progress, isSearchAgain = false) ?: return null
        return AddGameRequest(
            metaData = MetaData(library.id, directory, lastModified = DateTime.now()),
            providerData = providerData,
            userData = null
        )
    }
}
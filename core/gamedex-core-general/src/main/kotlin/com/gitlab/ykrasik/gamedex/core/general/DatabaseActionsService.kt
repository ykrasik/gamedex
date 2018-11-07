/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.general

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.general.StaleCache
import com.gitlab.ykrasik.gamedex.app.api.general.StaleData
import com.gitlab.ykrasik.gamedex.app.api.util.Task
import com.gitlab.ykrasik.gamedex.app.api.util.task
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.image.ImageService
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.util.create
import com.gitlab.ykrasik.gamedex.util.emptyToNull
import com.google.inject.ImplementedBy
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/04/2018
 * Time: 18:11
 */
@ImplementedBy(DatabaseActionsServiceImpl::class)
interface DatabaseActionsService {
    fun importDatabase(file: File): Task<Unit>
    fun exportDatabase(file: File): Task<Unit>

    fun detectStaleCache(): Task<StaleCache>
    fun deleteStaleCache(staleCache: StaleCache): Task<Unit>

    fun detectStaleData(): Task<StaleData>
    fun deleteStaleData(staleData: StaleData): Task<Unit>

    fun deleteAllUserData(): Task<Unit>
}

@Singleton
class DatabaseActionsServiceImpl @Inject constructor(
    private val libraryService: LibraryService,
    private val gameService: GameService,
    private val imageService: ImageService,
    private val fileSystemService: FileSystemService,
    private val persistenceService: PersistenceService
) : DatabaseActionsService {
    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .registerModule(JodaModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)

    override fun importDatabase(file: File) = task("Importing Database...") {
        message = "Reading $file..."
        val portableDb: PortableDb = objectMapper.readValue(file, PortableDb::class.java)

        message = "Deleting existing database..."
        persistenceService.dropDb()   // TODO: Is it possible to hide all access to persistenceService somehow?
        gameService.invalidate()
        libraryService.invalidate()
        fileSystemService.invalidate()

        message = "Importing database..."
        val libraries: Map<Int, Library> = executeSubTask(libraryService.addAll(portableDb.libraries.map { it.toLibraryData() }))
            .associateBy { library -> portableDb.findLib(library.path, library.platform).id }

        executeSubTask(gameService.addAll(portableDb.games.map { it.toGameRequest(libraries) }))

        successMessage = { "Database imported: ${portableDb.libraries.size} Libraries, ${portableDb.games.size} Games." }
    }

    override fun exportDatabase(file: File) = task("Exporting Database...") {
        val libraries = libraryService.libraries.mapWithProgress { it.toPortable() }
        val games = gameService.games.sortedBy { it.name }.mapWithProgress { it.toPortable() }

        message = "Writing $file..."
        file.create()
        val portableDb = PortableDb(libraries, games)
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, portableDb)

        successMessage = { "Database exported: ${file.absolutePath}" }
    }

    override fun detectStaleCache() = task("Detecting stale cache...") {
        val images = run {
            val usedImages = gameService.games.flatMapWithProgress { game ->
                game.imageUrls.screenshotUrls + listOfNotNull(game.imageUrls.thumbnailUrl, game.imageUrls.posterUrl)
            }
            imageService.fetchImageSizesExcept(usedImages)
        }

        val fileStructure = fileSystemService.getFileStructureSizeTakenExcept(gameService.games)

        StaleCache(images, fileStructure).apply {
            successMessage = { if (isEmpty) "No stale cache detected." else "Stale Cache:\n${toFormattedString()}" }
        }
    }

    override fun deleteStaleCache(staleCache: StaleCache) = task("Deleting Stale Cache...") {
        totalItems = 2

        staleCache.images.toList().emptyToNull()?.let { images ->
            message = "Deleting stale images..."
            imageService.deleteImages(images.map { it.first })
        }
        incProgress()

        staleCache.fileStructure.toList().emptyToNull()?.let { fileStructure ->
            message = "Deleting stale file structure..."
            fileStructure.forEach { fileSystemService.deleteStructure(it.first) }
        }
        incProgress()

        successMessage = { "Deleted Stale Cache:\n${staleCache.toFormattedString()}" }
    }

    override fun detectStaleData() = task("Detecting stale data...") {
        val libraries = libraryService.libraries.filterWithProgress { !it.path.isDirectory }
        val games = gameService.games.filterWithProgress { !it.path.isDirectory }

        val staleCache = executeSubTask(detectStaleCache())

        StaleData(libraries, games, staleCache).apply {
            successMessage = { if (isEmpty) "No stale data detected." else "Stale Data:\n${toFormattedString()}" }
        }
    }

    override fun deleteStaleData(staleData: StaleData) = task("Deleting Stale Data...") {
        totalItems = 3

        staleData.games.emptyToNull()?.let { games ->
            executeSubTask(gameService.deleteAll(games))
        }
        incProgress()

        staleData.libraries.emptyToNull()?.let { libraries ->
            executeSubTask(libraryService.deleteAll(libraries))
        }
        incProgress()

        executeSubTask(deleteStaleCache(staleData.staleCache))
        incProgress()

        successMessage = { "Deleted Stale Data:\n${staleData.toFormattedString()}" }
    }

    private fun StaleCache.toFormattedString() =
        listOf(images to "Images", fileStructure to "File Structure Entries")
            .asSequence()
            .filter { it.first.isNotEmpty() }
            .joinToString("\n") { "${it.first.size} ${it.second}" }

    private fun StaleData.toFormattedString() =
        listOf(games to "Games", libraries to "Libraries")
            .asSequence()
            .filter { it.first.isNotEmpty() }
            .joinToString("\n") { "${it.first.size} ${it.second}" } +
            staleCache.toFormattedString()

    override fun deleteAllUserData() = gameService.deleteAllUserData()
}
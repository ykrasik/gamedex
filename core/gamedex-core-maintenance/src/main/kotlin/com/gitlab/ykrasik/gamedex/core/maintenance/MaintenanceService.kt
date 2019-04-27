/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.maintenance

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.app.api.maintenance.*
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.image.ImageService
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.maintenance.PortableDb.toPortable
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.task.task
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.create
import com.gitlab.ykrasik.gamedex.util.emptyToNull
import com.gitlab.ykrasik.gamedex.util.toMultiMap
import com.google.inject.ImplementedBy
import difflib.DiffUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/04/2018
 * Time: 18:11
 */
@ImplementedBy(MaintenanceServiceImpl::class)
interface MaintenanceService {
    fun importDatabase(file: File): Task<Unit>
    fun exportDatabase(file: File): Task<Unit>

    fun detectStaleData(): Task<StaleData>
    fun deleteStaleData(staleData: StaleData): Task<Unit>

    fun deleteAllUserData(): Task<Unit>

    fun detectDuplicates(): Task<List<GameDuplicates>>

    fun detectFolderNameDiffs(): Task<List<FolderNameDiffs>>
}

@Singleton
class MaintenanceServiceImpl @Inject constructor(
    private val libraryService: LibraryService,
    private val gameService: GameService,
    private val imageService: ImageService,
    private val fileSystemService: FileSystemService,
    private val persistenceService: PersistenceService,
    private val eventBus: EventBus
) : MaintenanceService {
    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .registerModule(JodaModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)

    override fun importDatabase(file: File) = task("Importing Database...") {
        message = "Reading $file..."
        val portableDb: PortableDb.Db = objectMapper.readValue(file, PortableDb.Db::class.java)

        message = "Deleting existing database..."
        persistenceService.dropDb()   // TODO: Is it possible to hide all access to persistenceService somehow?
        eventBus.send(DatabaseInvalidatedEvent).join()

        message = "Importing database..."
        val libraries: Map<Int, Library> = executeSubTask(libraryService.addAll(portableDb.libraries.map { it.toLibraryData() }))
            .associateBy { library -> portableDb.findLib(library.path, library.type, library.platformOrNull).id }

        executeSubTask(gameService.addAll(portableDb.games.map { it.toGameRequest(libraries) }))

        successMessage = { "Database imported: ${portableDb.libraries.size} Libraries, ${portableDb.games.size} Games." }
    }

    override fun exportDatabase(file: File) = task("Exporting Database...") {
        val libraries = libraryService.libraries.mapWithProgress { it.toPortable() }
        val games = gameService.games.sortedBy { it.name }.mapWithProgress { it.toPortable() }

        message = "Writing $file..."
        file.create()
        val portableDb = PortableDb.Db(libraries, games)
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, portableDb)

        successMessage = { "Database exported: ${file.absolutePath}" }
    }

    override fun detectStaleData() = task("Detecting stale data...") {
        val libraries = libraryService.libraries.filterWithProgress { !it.path.isDirectory }

        val games = gameService.games.filterWithProgress { !it.path.isDirectory }

        val images = run {
            val usedImages = gameService.games.flatMapWithProgress { game ->
                game.imageUrls.screenshotUrls + listOfNotNull(game.imageUrls.thumbnailUrl, game.imageUrls.posterUrl)
            }
            imageService.fetchImageSizesExcept(usedImages)
        }

        val fileTree = fileSystemService.getFileTreeSizeTakenExcept(gameService.games)

        val staleData = StaleData(libraries, games, images, fileTree)
        if (staleData.isEmpty) {
            successMessage = { "No stale data detected." }
        }
        staleData
    }

    override fun deleteStaleData(staleData: StaleData) = task("Deleting Stale Data...") {
        totalItems = 4

        staleData.games.emptyToNull()?.let { games ->
            message = "Deleting stale games..."
            executeSubTask(gameService.deleteAll(games))
        }
        incProgress()

        staleData.libraries.emptyToNull()?.let { libraries ->
            message = "Deleting stale libraries..."
            executeSubTask(libraryService.deleteAll(libraries))
        }
        incProgress()

        staleData.images.toList().emptyToNull()?.let { images ->
            message = "Deleting stale images..."
            imageService.deleteImages(images.map { it.first })
        }
        incProgress()

        staleData.fileTrees.toList().emptyToNull()?.let { fileTrees ->
            message = "Deleting stale file cache..."
            fileTrees.forEach { fileSystemService.deleteCachedFileTree(it.first) }
        }
        incProgress()

        successMessage = { "Deleted Stale Data:\n${staleData.toFormattedString()}" }
    }

    private fun StaleData.toFormattedString() =
        listOf(games.size to "Games", libraries.size to "Libraries", images.size to "Images", fileTrees.size to "File Cache")
            .asSequence()
            .filter { it.first != 0 }
            .joinToString("\n") { "${it.first} ${it.second}" }

    override fun deleteAllUserData() = gameService.deleteAllUserData()


    // TODO: Add ignore case option
    // TODO: Add option that makes metadata an optional match.
    override fun detectDuplicates() = task("Detecting duplicate games...") {
        val headerToGames = gameService.games.asSequence()
            .flatMap { game -> game.providerHeaders.map { it to game } }
            .toMultiMap()

        // Only detect duplicates in the same platform.
        val duplicateHeaders = headerToGames.mapValues { (_, games) ->
            games.groupBy { it.platform }.flatMap { (_, games) -> if (games.size > 1) games else emptyList() }
        }.filterValues { it.take(2).toList().size > 1 }

        totalItems = duplicateHeaders.values.sumBy { it.size }

        val gameIdsToDuplicates = mutableMapOf<GameId, MutableList<Pair<Game, ProviderId>>>()
        duplicateHeaders.forEach { (header, games) ->
            games.forEach { game1 ->
                val duplicates = gameIdsToDuplicates.getOrPut(game1.id) { mutableListOf() }
                games.forEach { game2 ->
                    if (game1.id != game2.id) {
                        duplicates += game2 to header.id
                    }
                }
                incProgress()
            }
        }

        gameIdsToDuplicates.map { (gameId, duplicates) ->
            GameDuplicates(gameService[gameId], duplicates.map { (game, providerId) -> GameDuplicate(game, providerId) })
        }
    }

    override fun detectFolderNameDiffs() = task("Detecting game folder name diffs...") {
        gameService.games.mapNotNull { game ->
            val diffs = game.rawGame.providerData.mapNotNull { providerData ->
                diff(game.folderName, providerData)
            }
            // TODO: If a majority of the providers agree on the name, this should not be a diff.
            if (diffs.isNotEmpty()) FolderNameDiffs(game, diffs) else null
        }
    }

    private fun diff(folderName: FolderName, providerData: ProviderData): FolderNameDiff? {
        val expectedFolderName = providerData.toExpectedName(folderName)
        if (folderName.rawName == expectedFolderName) return null

        val patch = DiffUtils.diff(folderName.rawName.toList(), expectedFolderName.toList())
        return FolderNameDiff(
            providerId = providerData.header.id,
            folderName = folderName.rawName,
            expectedFolderName = expectedFolderName,
            patch = patch
        )
    }

    private fun ProviderData.toExpectedName(folderName: FolderName): String {
        val expected = StringBuilder()
        folderName.order?.let { order -> expected.append("[$order] ") }
        expected.append(fileSystemService.toFileName(gameData.name))
        folderName.metaTag?.let { metaTag -> expected.append(" [$metaTag]") }
        folderName.version?.let { version -> expected.append(" [$version]") }
        return expected.toString()
    }
}
/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.app.api.maintenance.*
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.filter.FilterService
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.image.ImageService
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.library.SyncLibraryService
import com.gitlab.ykrasik.gamedex.core.maintenance.PortableDb.toPortable
import com.gitlab.ykrasik.gamedex.core.maintenance.PortableFilter.toPortable
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.core.settings.ProviderSettingsRepository
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.task.task
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.*
import com.google.inject.ImplementedBy
import difflib.DiffUtils
import info.debatty.java.stringsimilarity.JaroWinkler
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 01/04/2018
 * Time: 18:11
 */
@ImplementedBy(MaintenanceServiceImpl::class)
interface MaintenanceService {
    fun exportDatabase(dir: File, params: ImportExportParams): Task<File>

    fun readImportDbFile(file: File): Task<ImportDbContent>
    fun importDatabase(content: ImportDbContent): Task<Unit>

    fun detectCleanupData(): Task<CleanupData>
    fun fixCleanupData(cleanupData: CleanupData): Task<Unit>

    fun deleteAllUserData(): Task<Unit>

    fun detectDuplicates(): Task<List<GameDuplicates>>

    fun detectFolderNameDiffs(): Task<List<FolderNameDiffs>>
}

data class ImportExportParams(
    val library: Boolean,
    val providerAccounts: Boolean,
    val filters: Boolean,
)

data class ImportDbContent(
    val db: PortableDb.Db?,
    val accounts: PortableProviderAccounts?,
    val filters: PortableFilter.Filters?,
)

data class PortableProviderAccounts(
    val accounts: Map<ProviderId, Map<String, String>>,
)

@Singleton
class MaintenanceServiceImpl @Inject constructor(
    private val libraryService: LibraryService,
    private val syncLibraryService: SyncLibraryService,
    private val gameService: GameService,
    private val imageService: ImageService,
    private val fileSystemService: FileSystemService,
    private val persistenceService: PersistenceService,
    private val settingsRepo: ProviderSettingsRepository,
    private val filterService: FilterService,
    private val eventBus: EventBus,
) : MaintenanceService {
    private val log = logger()

    private val objectMapper = jacksonObjectMapper()
        .registerModule(JodaModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)

    private val jaroWinkler = JaroWinkler()

    override fun exportDatabase(dir: File, params: ImportExportParams) = task("Exporting Database...") {
        val file = dir.resolve("GameDex ${now.defaultTimeZone.toString("yyyy-MM-dd HH_mm_ss")}.zip")
        file.create()
        successMessage = { file.absolutePath }

        ZipOutputStream(file.outputStream()).use { zos ->
            fun write(entry: String, data: Any) {
                zos.putNextEntry(ZipEntry(entry))
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(zos, data)
            }

            totalItems.value = 3

            if (params.library) {
                withMessage("Exporting Library...") {
                    val portableDb = PortableDb.Db(
                        libraries = libraryService.libraries.map { it.toPortable() },
                        games = gameService.games.sortedBy { it.name }.map { it.toPortable() }
                    )
                    write(libraryEntry, portableDb)
                }
            }
            incProgress()

            if (params.providerAccounts) {
                withMessage("Exporting Provider Accounts...") {
                    val accounts = PortableProviderAccounts(
                        settingsRepo.providers
                            .filter { (_, repo) -> repo.account.value.isNotEmpty() }
                            .mapValues { (_, repo) -> repo.account.value }
                    )
                    write(providerAccountsEntry, accounts)
                }
            }
            incProgress()

            if (params.filters) {
                withMessage("Exporting Filters...") {
                    val filters = PortableFilter.Filters(filterService.userFilters.map { it.toPortable() })
                    write(filtersEntry, filters)
                }
            }
            incProgress()
        }

        file
    }

    override fun readImportDbFile(file: File): Task<ImportDbContent> = task("Reading $file...") {
        successMessage = null
        check(RandomAccessFile(file, "r").readInt() == zipMagicHeader) { "Not a zip file: $file!" }
        ZipInputStream(file.inputStream()).use { zis ->
            fun <T : Any> read(klass: KClass<T>): T = objectMapper.readValue(zis.readBytes(), klass.java)

            var db: PortableDb.Db? = null
            var accounts: PortableProviderAccounts? = null
            var filters: PortableFilter.Filters? = null

            var entry = zis.nextEntry
            while (entry != null) {
                when (entry.name) {
                    libraryEntry -> db = read(PortableDb.Db::class)
                    providerAccountsEntry -> accounts = read(PortableProviderAccounts::class)
                    filtersEntry -> filters = read(PortableFilter.Filters::class)
                }
                entry = zis.nextEntry
            }
            zis.closeEntry()

            ImportDbContent(db, accounts, filters)
        }
    }

    override fun importDatabase(content: ImportDbContent) = task("Importing Database...") {
        totalItems.value = 3

        content.db?.let { db ->
            withMessage("Importing Library...") {
                persistenceService.dropDb()
                eventBus.emit(DatabaseInvalidatedEvent)

                val libraries: Map<Int, Library> = executeSubTask(libraryService.addAll(db.libraries.map { it.toLibraryData() }))
                    .associateBy { library -> db.findLib(library.path, library.type, library.platformOrNull).id }

                executeSubTask(gameService.addAll(db.games.map { it.toGameRequest(libraries) }))
            }
        }
        incProgress()

        content.accounts?.let { accounts ->
            withMessage("Importing Provider Accounts...") {
                accounts.accounts.forEach { (providerId, account) ->
                    if (account.isNotEmpty()) {
                        settingsRepo.providers[providerId]?.account?.value = account
                    }
                }
            }
        }
        incProgress()

        content.filters?.let { filters ->
            withMessage("Importing Filters...") {
                executeSubTask(filterService.saveAll(filters.filters.map { it.toDomain() }))
            }
        }
        incProgress()
    }

    override fun detectCleanupData() = task("Detecting cleanup data...") {
        totalItems.value = 5

        val missingLibraries = libraryService.libraries.filter { !it.path.isDirectory }
        incProgress()

        val missingGames = gameService.games.filter { !it.path.isDirectory }
        incProgress()

        val movedGames = detectMovedGames(missingGames)
        incProgress()

        val staleImages = run {
            val usedImages = gameService.games.flatMapTo(mutableSetOf()) { game ->
                game.screenshotUrls + listOfNotNull(game.thumbnailUrl, game.posterUrl)
            }
            imageService.fetchImageSizesExcept(usedImages)
        }
        incProgress()

        val staleFileTrees = fileSystemService.getFileTreeSizeTakenExcept(gameService.games)
        incProgress()

        val cleanupData = CleanupData(movedGames, missingLibraries, missingGames, staleImages, staleFileTrees)

        successMessage = if (cleanupData.isEmpty) {
            { "No stale data detected." }
        } else {
            null
        }
        cleanupData
    }

    private suspend fun Task<*>.detectMovedGames(missingGames: List<Game>): List<Pair<Game, LibraryPath>> {
        val newPaths = executeSubTask(syncLibraryService.detectNewPaths())
        if (newPaths.isEmpty()) {
            return emptyList()
        }

        val newPathFolderNames = newPaths.map { it to fileSystemService.analyzeFolderName(it.path.name) }
        return missingGames.mapNotNull { game ->
            val newPath = detectMoveTarget(game, newPathFolderNames)
            if (newPath != null) {
                game to newPath
            } else {
                null
            }
        }
    }

    private fun detectMoveTarget(game: Game, newPaths: List<Pair<LibraryPath, FolderName>>): LibraryPath? {
        val movedWithoutRename = newPaths.find { (path) -> path.path.name == game.folderName.rawName }
        if (movedWithoutRename != null) {
            log.debug("Detected move without renaming: ${game.path} -> ${movedWithoutRename.first.path}")
            return movedWithoutRename.first
        }

        val renamedMetadata = newPaths.find { (_, folderName) ->
            game.folderName.isSameBaseName(folderName)
        }
        if (renamedMetadata != null) {
            log.debug("Detected renamed metadata: ${game.path} -> ${renamedMetadata.first.path}")
            return renamedMetadata.first
        }

        val mostSimilar = newPaths.asSequence()
            .map { (path, folderName) ->
                val similarity = jaroWinkler.similarity(
                    game.folderName.processedName,
                    folderName.processedName,
                )
                Triple(path, folderName, similarity)
            }
            .sortedByDescending { (_, _, similarity) -> similarity }
            .firstOrNull()
        if (mostSimilar != null && mostSimilar.third >= renameSimilarityThreshold) {
            log.debug("Detected rename to similar name: ${game.path} -> ${mostSimilar.first.path} [${mostSimilar.third}%]")
            return mostSimilar.first
        }

        return null
    }

    override fun fixCleanupData(cleanupData: CleanupData) = task("Cleaning up data...") {
        totalItems.value = 5

        val missingGames =
            if (cleanupData.movedGames.isNotEmpty()) {
                cleanupData.missingGames.filterNot { missingGame ->
                    cleanupData.movedGames.any { (game) -> game.id == missingGame.id }
                }
            } else {
                cleanupData.missingGames
            }
        missingGames.emptyToNull()?.let { games ->
            message.value = "Deleting stale games..."
            executeSubTask(gameService.deleteAll(games))
        }
        incProgress()

        cleanupData.missingLibraries.emptyToNull()?.let { libraries ->
            message.value = "Deleting stale libraries..."
            executeSubTask(libraryService.deleteAll(libraries))
        }
        incProgress()

        cleanupData.movedGames.emptyToNull()?.let { movedGames ->
            message.value = "Fixing moved games..."
            executeSubTask(task("Updating ${movedGames.size} games...") {
                movedGames.forEachWithProgress { (game, newPath) ->
                    gameService.replace(
                        game,
                        game.rawGame.withMetadata {
                            it.copy(
                                libraryId = newPath.library.id,
                                path = newPath.relativePath.path
                            )
                        }).execute()
                }
            })

        }
        incProgress()

        cleanupData.staleImages.toList().emptyToNull()?.let { images ->
            message.value = "Deleting stale images..."
            imageService.deleteImages(images.map { it.first })
        }
        incProgress()

        cleanupData.staleFileTrees.toList().emptyToNull()?.let { fileTrees ->
            message.value = "Deleting stale file cache..."
            fileTrees.forEach { fileSystemService.deleteCachedFileTree(it.first) }
        }
        incProgress()

        successMessage = { cleanupData.toFormattedString() }
    }

    private fun CleanupData.toFormattedString() =
        sequenceOf(
            missingGames.size to "Games",
            missingLibraries.size to "Libraries",
            staleImages.size to "Images",
            staleFileTrees.size to "File Cache"
        ).filter { it.first != 0 }
            .joinToString("\n") { "${it.first} ${it.second}" }

    override fun deleteAllUserData() = gameService.deleteAllUserData()


    // TODO: Add ignore case option
    // TODO: Add option that makes metadata an optional match.
    override fun detectDuplicates() = task("Detecting duplicate games...") {
        successMessage = null

        val headerToGames = gameService.games.asSequence()
            .flatMap { game -> game.providerHeaders.map { it to game } }
            .toMultiMap()

        // Only detect duplicates in the same platform.
        val duplicateHeaders = headerToGames.mapValues { (_, games) ->
            games.groupBy { it.platform }.flatMap { (_, games) -> if (games.size > 1) games else emptyList() }
        }.filterValues { it.take(2).toList().size > 1 }

        totalItems.value = duplicateHeaders.values.sumBy { it.size }

        val gameIdsToDuplicates = mutableMapOf<GameId, MutableList<Pair<Game, ProviderId>>>()
        duplicateHeaders.forEach { (header, games) ->
            games.forEach { game1 ->
                val duplicates = gameIdsToDuplicates.getOrPut(game1.id) { mutableListOf() }
                games.forEach { game2 ->
                    if (game1.id != game2.id) {
                        duplicates += game2 to header.providerId
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
        successMessage = null

        gameService.games.mapNotNull { game ->
            val diffs = game.providerData.map { providerData ->
                diff(game.folderName, providerData)
            }
            // TODO: If a majority of the providers agree on the name, this should not be a diff.
            if (diffs.any { it.patch != null }) {
                FolderNameDiffs(game, diffs.sortedBy { it.providerId })
            } else {
                null
            }
        }
    }

    private fun diff(folderName: FolderName, providerData: ProviderData): FolderNameDiff {
        val expectedFolderName = providerData.toExpectedName(folderName)
        val patch = if (folderName.rawName != expectedFolderName) {
            DiffUtils.diff(folderName.rawName.toList(), expectedFolderName.toList())
        } else {
            null
        }

        return FolderNameDiff(
            providerId = providerData.providerId,
            folderName = folderName.rawName,
            expectedFolderName = expectedFolderName,
            patch = patch
        )
    }

    private fun ProviderData.toExpectedName(folderName: FolderName): String {
        val expected = StringBuilder()
        folderName.order?.let { order -> expected.append("[$order] ") }
        expected.append(fileSystemService.sanitizeFileName(gameData.name))
        folderName.metaTag?.let { metaTag -> expected.append(" [$metaTag]") }
        folderName.version?.let { version -> expected.append(" [$version]") }
        return expected.toString()
    }

    companion object {
        private const val zipMagicHeader = 0x504B0304
        private const val libraryEntry = "library.json"
        private const val providerAccountsEntry = "accounts.json"
        private const val filtersEntry = "filters.json"
        private const val renameSimilarityThreshold = 0.7
    }
}
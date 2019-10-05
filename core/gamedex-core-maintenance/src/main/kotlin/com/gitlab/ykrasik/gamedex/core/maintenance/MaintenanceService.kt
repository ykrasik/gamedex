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

    fun detectStaleData(): Task<StaleData>
    fun deleteStaleData(staleData: StaleData): Task<Unit>

    fun deleteAllUserData(): Task<Unit>

    fun detectDuplicates(): Task<List<GameDuplicates>>

    fun detectFolderNameDiffs(): Task<List<FolderNameDiffs>>
}

data class ImportExportParams(
    val library: Boolean,
    val providerAccounts: Boolean,
    val filters: Boolean
)

data class ImportDbContent(
    val db: PortableDb.Db?,
    val accounts: PortableProviderAccounts?,
    val filters: PortableFilter.Filters?
)

data class PortableProviderAccounts(
    val accounts: Map<ProviderId, Map<String, String>>
)

@Singleton
class MaintenanceServiceImpl @Inject constructor(
    private val libraryService: LibraryService,
    private val gameService: GameService,
    private val imageService: ImageService,
    private val fileSystemService: FileSystemService,
    private val persistenceService: PersistenceService,
    private val settingsRepo: ProviderSettingsRepository,
    private val filterService: FilterService,
    private val eventBus: EventBus
) : MaintenanceService {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JodaModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)

    override fun exportDatabase(dir: File, params: ImportExportParams) = task("Exporting Database...") {
        val file = dir.resolve("GameDex ${now.defaultTimeZone.toString("yyyy-MM-dd HH_mm_ss")}.zip")
        file.create()
        successMessage = { file.absolutePath }

        ZipOutputStream(file.outputStream()).use { zos ->
            fun write(entry: String, data: Any) {
                zos.putNextEntry(ZipEntry(entry))
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(zos, data)
            }

            totalItems = 3

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
                            .filter { (_, repo) -> repo.account.isNotEmpty() }
                            .mapValues { (_, repo) -> repo.account }
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
                when {
                    entry.name == libraryEntry -> db = read(PortableDb.Db::class)
                    entry.name == providerAccountsEntry -> accounts = read(PortableProviderAccounts::class)
                    entry.name == filtersEntry -> filters = read(PortableFilter.Filters::class)
                }
                entry = zis.nextEntry
            }
            zis.closeEntry()

            ImportDbContent(db, accounts, filters)
        }
    }

    override fun importDatabase(content: ImportDbContent) = task("Importing Database...") {
        totalItems = 3

        content.db?.let { db ->
            withMessage("Importing Library...") {
                persistenceService.dropDb()
                eventBus.send(DatabaseInvalidatedEvent).join()

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
                        settingsRepo.providers[providerId]?.modify { copy(account = account) }
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

    override fun detectStaleData() = task("Detecting stale data...") {
        val libraries = libraryService.libraries.filterWithProgress { !it.path.isDirectory }

        val games = gameService.games.filterWithProgress { !it.path.isDirectory }

        val images = run {
            val usedImages = gameService.games.flatMapWithProgress { game ->
                game.screenshotUrls + listOfNotNull(game.thumbnailUrl, game.posterUrl)
            }
            imageService.fetchImageSizesExcept(usedImages)
        }

        val fileTree = fileSystemService.getFileTreeSizeTakenExcept(gameService.games)

        val staleData = StaleData(libraries, games, images, fileTree)

        successMessage = if (staleData.isEmpty) {
            { "No stale data detected." }
        } else {
            null
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

        successMessage = { staleData.toFormattedString() }
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
        successMessage = null

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
        expected.append(fileSystemService.toFileName(gameData.name))
        folderName.metaTag?.let { metaTag -> expected.append(" [$metaTag]") }
        folderName.version?.let { version -> expected.append(" [$version]") }
        return expected.toString()
    }

    companion object {
        private const val zipMagicHeader = 0x504B0304
        private const val libraryEntry = "library.json"
        private const val providerAccountsEntry = "accounts.json"
        private const val filtersEntry = "filters.json"
    }
}
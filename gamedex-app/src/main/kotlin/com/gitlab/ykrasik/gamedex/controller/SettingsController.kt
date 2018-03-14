package com.gitlab.ykrasik.gamedex.controller

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonGenerator.Feature.IGNORE_UNKNOWN
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.repository.AddGameRequest
import com.gitlab.ykrasik.gamedex.repository.AddLibraryRequest
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.repository.LibraryRepository
import com.gitlab.ykrasik.gamedex.settings.AllSettings
import com.gitlab.ykrasik.gamedex.task.DatabaseTasks
import com.gitlab.ykrasik.gamedex.ui.Task
import com.gitlab.ykrasik.gamedex.ui.fitAtMost
import com.gitlab.ykrasik.gamedex.ui.view.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.view.main.MainView
import com.gitlab.ykrasik.gamedex.ui.view.settings.SettingsView
import com.gitlab.ykrasik.gamedex.util.*
import javafx.beans.binding.BooleanBinding
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.joda.time.DateTimeZone
import tornadofx.*
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 09/05/2017
 * Time: 17:09
 */
@Singleton
class SettingsController @Inject constructor(
    private val gameRepository: GameRepository,
    private val libraryRepository: LibraryRepository,
    private val persistenceService: PersistenceService,
    private val settings: AllSettings,
    private val databaseTasks: DatabaseTasks
) : Controller() {
    private val logger = logger()

    private val settingsView: SettingsView by inject()

    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .registerModule(JodaModule())
        .configure(WRITE_DATES_AS_TIMESTAMPS, true)
        .configure(IGNORE_UNKNOWN, true)

    val canRunLongTask: BooleanBinding get() = MainView.canShowPersistentNotificationProperty

    fun providerSettings(providerId: ProviderId) = settings.provider[providerId]
    fun setProviderEnabled(providerId: ProviderId, enable: Boolean) {
        settings.provider.modify(providerId) { copy(enable = enable) }
    }

    fun showSettingsMenu() {
        settings.saveSnapshot()
        try {
            val accept = settingsView.show()
            if (accept) {
                settings.commitSnapshot()
            } else {
                settings.restoreSnapshot()
            }
        } catch (e: Exception) {
            logger.error("Error updating settings!", e)
            settings.restoreSnapshot()
        }
    }

    suspend fun validateAndUseAccount(provider: GameProvider, account: Map<String, String>): Boolean = withContext(CommonPool) {
        val newAccount = provider.accountFeature!!.createAccount(account)
        val valid = validate(provider, newAccount)
        if (valid) {
            withContext(JavaFx) {
                settings.provider.modify(provider.id) { copy(account = account) }
            }
        }
        valid
    }

    private fun validate(provider: GameProvider, account: ProviderUserAccount): Boolean = try {
        logger.info { "[${provider.id}] Validating: $account" }
        provider.search("TestSearchToVerifyAccount", Platform.pc, account)
        logger.info { "[${provider.id}] Valid!" }
        true
    } catch (e: Exception) {
        logger.warn("[${provider.id}] Invalid!", e)
        false
    }

    fun exportDatabase() {
        val dir = browseDirectory() ?: return
        val timestamp = now.withZone(DateTimeZone.getDefault())
        val timestamptedPath = Paths.get(
            dir.toString(),
            timestamp.toString("yyyy-MM-dd"),
            "db_${timestamp.toString("HH_mm_ss")}.json"
        ).toFile()
        timestamptedPath.create()
        ExportDatabaseTask(timestamptedPath).start()
        browse(timestamptedPath.parentFile)
    }

    fun importDatabase() {
        val file = browseFile(FileChooserMode.Single) ?: return
        if (!areYouSureDialog("This will overwrite the existing database.")) return
        ImportDatabaseTask(file).start()
    }

    private fun browseDirectory(): File? {
        val dir = chooseDirectory("Choose database export directory...", initialDirectory = settings.general.exportDbDirectory)
            ?: return null
        settings.general.exportDbDirectory = dir
        return dir
    }

    private fun browseFile(mode: FileChooserMode): File? {
        val file = chooseFile("Choose database file...", filters = emptyArray(), mode = mode) {
            initialDirectory = settings.general.exportDbDirectory
            initialFileName = "db.json"
        }
        return file.firstOrNull()
    }

    inner class ExportDatabaseTask(private val file: File) : Task<Unit>("Exporting Database...") {
        override suspend fun doRun() {
            val libraries = libraryRepository.libraries.map { it.toPortable() }
            val games = gameRepository.games.sortedBy { it.name }.map { it.toPortable() }

            progress.message = "Writing file..."
            val portableDb = PortableDb(libraries, games)
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, portableDb)
        }

        override fun doneMessage() = "Done: Exported ${gameRepository.games.size} games."
    }

    // TODO: This isn't actually cancellable. Do I want it to be? Should probably use a maskerPane instead.
    // TODO: Split long running tasks into cancellable and non-cancellable.
    inner class ImportDatabaseTask(private val file: File) : Task<Unit>("Importing Database...") {
        private var importedGames = 0

        override suspend fun doRun() {
            val portableDb = objectMapper.readValue(file, PortableDb::class.java)
            persistenceService.dropDb()
            libraryRepository.invalidate()
            gameRepository.hardInvalidate()

            val libraries = libraryRepository.addAll(portableDb.libraries.map { it.toLibraryRequest() })
                .associateBy { lib -> portableDb.findLib(lib.path, lib.platform).id }

            val requests = portableDb.games.map { it.toGameRequest(libraries) }

            gameRepository.addAll(requests, progress)
            importedGames = requests.size
        }

        override fun doneMessage() = "Done: Imported $importedGames games."
    }

    fun cleanupDb() = launch(JavaFx) {
        val staleData = databaseTasks.DetectStaleDataTask().apply { start() }.result.await()
        if (confirmCleanup(staleData)) {
            // TODO: Create backup before deleting
            databaseTasks.CleanupStaleDataTask(staleData).apply { start() }
        }
    }

    private fun confirmCleanup(staleData: DatabaseTasks.StaleData): Boolean {
        if (staleData.isEmpty) return false

        val (libraries, games, images) = staleData

        return areYouSureDialog("Delete the following stale data?") {
            if (libraries.isNotEmpty()) {
                label("Stale Libraries: ${libraries.size}")
                listview(libraries.map { it.path }.observable()) { fitAtMost(5) }
            }
            if (games.isNotEmpty()) {
                label("Stale Games: ${games.size}")
                listview(games.map { it.path }.observable()) { fitAtMost(5) }
            }
            if(images.isNotEmpty()) {
                label("Stale Images: ${images.size} (${staleData.staleImagesSize})")
                listview(images.map { it.first }.observable()) { fitAtMost(5) }
            }
        }
    }

    private data class PortableDb(
        val libraries: List<PortableLibrary>,
        val games: List<PortableGame>
    ) {
        fun findLib(path: File, platform: Platform) = libraries.find { it.path == path.path && it.platform == platform.name }!!
    }

    private data class PortableLibrary(
        val id: Int,
        val path: String,
        val platform: String,
        val name: String
    ) {
        fun toLibraryRequest() = AddLibraryRequest(
            path = path.toFile(),
            data = LibraryData(
                platform = Platform.valueOf(platform),
                name = name
            )
        )
    }

    private fun Library.toPortable() = PortableLibrary(
        id = id,
        path = path.toString(),
        platform = platform.name,
        name = name
    )

    private data class PortableGame(
        val libraryId: Int,
        val path: String,
        val updateDate: Long,
        val providerData: List<PortableProviderData>,
        val userData: PortableUserData?
    ) {
        fun toGameRequest(libraries: Map<Int, Library>) = AddGameRequest(
            metadata = Metadata(
                libraryId = libraries.getOrElse(libraryId) { throw IllegalArgumentException("Invalid library id: $libraryId") }.id,
                path = path,
                updateDate = updateDate.toDateTime()
            ),
            providerData = providerData.map { it.toProviderData() },
            userData = userData?.toUserData()
        )
    }

    private fun Game.toPortable() = PortableGame(
        libraryId = library.id,
        path = rawGame.metadata.path,
        updateDate = updateDate.millis,
        providerData = rawGame.providerData.map { it.toPortable() },
        userData = userData?.toPortable()
    )

    private data class PortableProviderData(
        val providerId: ProviderId,
        val apiUrl: String,
        val updateDate: Long,
        val siteUrl: String,
        val name: String,
        val description: String?,
        val releaseDate: String?,
        val criticScore: Double?,
        val numCriticReviews: Int?,
        val userScore: Double?,
        val numUserReviews: Int?,
        val genres: List<String>,
        val thumbnailUrl: String?,
        val posterUrl: String?,
        val screenshotUrls: List<String>
    ) {
        fun toProviderData() = ProviderData(
            header = ProviderHeader(
                id = providerId,
                apiUrl = apiUrl,
                updateDate = updateDate.toDateTime()
            ),
            gameData = GameData(
                siteUrl = siteUrl,
                name = name,
                description = description,
                releaseDate = releaseDate,
                criticScore = toScore(criticScore, numCriticReviews),
                userScore = toScore(userScore, numUserReviews),
                genres = genres,
                imageUrls = ImageUrls(
                    thumbnailUrl = thumbnailUrl,
                    posterUrl = posterUrl,
                    screenshotUrls = screenshotUrls
                )
            )
        )

        private fun toScore(score: Double?, numReviews: Int?) = score?.let { Score(it, numReviews!!) }
    }

    private fun ProviderData.toPortable() = PortableProviderData(
        providerId = header.id,
        apiUrl = header.apiUrl,
        updateDate = header.updateDate.millis,
        siteUrl = gameData.siteUrl,
        name = gameData.name,
        description = gameData.description,
        releaseDate = gameData.releaseDate,
        criticScore = gameData.criticScore?.score,
        numCriticReviews = gameData.criticScore?.numReviews,
        userScore = gameData.userScore?.score,
        numUserReviews = gameData.userScore?.numReviews,
        genres = gameData.genres,
        thumbnailUrl = gameData.imageUrls.thumbnailUrl,
        posterUrl = gameData.imageUrls.posterUrl,
        screenshotUrls = gameData.imageUrls.screenshotUrls
    )

    private data class PortableUserData(
        val nameOverride: PortableGameDataOverride?,
        val descriptionOverride: PortableGameDataOverride?,
        val releaseDateOverride: PortableGameDataOverride?,
        val criticScoreOverride: PortableGameDataOverride?,
        val userScoreOverride: PortableGameDataOverride?,
        val genresOverride: PortableGameDataOverride?,
        val thumbnailOverride: PortableGameDataOverride?,
        val posterOverride: PortableGameDataOverride?,
        val screenshotsOverride: PortableGameDataOverride?,
        val tags: List<String>,
        val excludedProviders: List<ProviderId>
    ) {
        fun toUserData(): UserData {
            val overrides = mutableMapOf<GameDataType, GameDataOverride>()
            nameOverride?.toOverride()?.let { overrides += GameDataType.name_ to it }
            descriptionOverride?.toOverride()?.let { overrides += GameDataType.description to it }
            releaseDateOverride?.toOverride()?.let { overrides += GameDataType.releaseDate to it }
            criticScoreOverride?.toOverride()?.let { overrides += GameDataType.criticScore to it }
            userScoreOverride?.toOverride()?.let { overrides += GameDataType.userScore to it }
            genresOverride?.toOverride()?.let { overrides += GameDataType.genres to it }
            thumbnailOverride?.toOverride()?.let { overrides += GameDataType.thumbnail to it }
            posterOverride?.toOverride()?.let { overrides += GameDataType.poster to it }
            screenshotsOverride?.toOverride()?.let { overrides += GameDataType.screenshots to it }

            return UserData(
                overrides = overrides,
                tags = tags,
                excludedProviders = excludedProviders
            )
        }
    }

    private fun UserData.toPortable() = PortableUserData(
        nameOverride = overrides[GameDataType.name_]?.toPortable(),
        descriptionOverride = overrides[GameDataType.description]?.toPortable(),
        releaseDateOverride = overrides[GameDataType.releaseDate]?.toPortable(),
        criticScoreOverride = overrides[GameDataType.criticScore]?.toPortable(),
        userScoreOverride = overrides[GameDataType.userScore]?.toPortable(),
        genresOverride = overrides[GameDataType.genres]?.toPortable(),
        thumbnailOverride = overrides[GameDataType.thumbnail]?.toPortable(),
        posterOverride = overrides[GameDataType.poster]?.toPortable(),
        screenshotsOverride = overrides[GameDataType.screenshots]?.toPortable(),
        tags = tags,
        excludedProviders = excludedProviders
    )

    private fun GameDataOverride.toPortable() = when (this) {
        is GameDataOverride.Provider -> PortableGameDataOverride.Provider(provider)
        is GameDataOverride.Custom -> PortableGameDataOverride.Custom(data)
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = PortableGameDataOverride.Provider::class, name = "provider"),
        JsonSubTypes.Type(value = PortableGameDataOverride.Custom::class, name = "custom")
    )
    private sealed class PortableGameDataOverride {
        abstract fun toOverride(): GameDataOverride

        data class Provider(val provider: ProviderId) : PortableGameDataOverride() {
            override fun toOverride() = GameDataOverride.Provider(provider)
        }

        data class Custom(val data: Any) : PortableGameDataOverride() {
            override fun toOverride() = GameDataOverride.Custom(data)
        }
    }
}
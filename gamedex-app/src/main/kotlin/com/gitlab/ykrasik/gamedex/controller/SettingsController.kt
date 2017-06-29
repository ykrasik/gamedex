package com.gitlab.ykrasik.gamedex.controller

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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
import com.gitlab.ykrasik.gamedex.settings.GeneralSettings
import com.gitlab.ykrasik.gamedex.ui.Task
import com.gitlab.ykrasik.gamedex.ui.view.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.util.*
import org.joda.time.DateTimeZone
import tornadofx.Controller
import tornadofx.FileChooserMode
import tornadofx.chooseDirectory
import tornadofx.chooseFile
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
    private val settings: GeneralSettings
) : Controller() {

    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .registerModule(JodaModule())
        .configure(WRITE_DATES_AS_TIMESTAMPS, true)

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
        val dir = chooseDirectory("Choose database export directory...", initialDirectory = settings.exportDbDirectory) ?: return null
        settings.exportDbDirectory = dir
        return dir
    }

    private fun browseFile(mode: FileChooserMode): File? {
        val file = chooseFile("Choose database file...", filters = emptyArray(), mode = mode) {
            initialDirectory = settings.exportDbDirectory
            initialFileName = "db.json"
        }
        return file.firstOrNull()
    }

    inner class ExportDatabaseTask(private val file: File) : Task<Unit>("Exporting Database...") {
        suspend override fun doRun() {
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

        suspend override fun doRun() {
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
                genres = genres
            ),
            imageUrls = ImageUrls(
                thumbnailUrl = thumbnailUrl,
                posterUrl = posterUrl,
                screenshotUrls = screenshotUrls
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
        thumbnailUrl = imageUrls.thumbnailUrl,
        posterUrl = imageUrls.posterUrl,
        screenshotUrls = imageUrls.screenshotUrls
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

    private fun GameDataOverride.toPortable() = when(this)  {
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
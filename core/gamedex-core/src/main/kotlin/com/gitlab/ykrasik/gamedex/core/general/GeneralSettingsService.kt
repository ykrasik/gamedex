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

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.app.api.general.StaleData
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.app.api.util.Task
import com.gitlab.ykrasik.gamedex.app.api.util.TaskType
import com.gitlab.ykrasik.gamedex.app.api.util.nonCancellableTask
import com.gitlab.ykrasik.gamedex.core.api.game.AddGameRequest
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.api.image.ImageRepository
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.create
import com.gitlab.ykrasik.gamedex.util.toDateTime
import com.gitlab.ykrasik.gamedex.util.toFile
import com.google.inject.ImplementedBy
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/04/2018
 * Time: 18:11
 */
@ImplementedBy(GeneralSettingsServiceImpl::class)
interface GeneralSettingsService {
    fun importDatabase(file: File): Task<Unit>
    fun exportDatabase(file: File): Task<Unit>

    suspend fun detectStaleData(): StaleData
    suspend fun deleteStaleData(staleData: StaleData)

    suspend fun deleteAllUserData()
}

@Singleton
class GeneralSettingsServiceImpl @Inject constructor(
    private val libraryService: LibraryService,
    private val gameService: GameService,
    private val imageRepository: ImageRepository,
    private val persistenceService: PersistenceService,
    private val taskRunner: TaskRunner
) : GeneralSettingsService {
    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .registerModule(JodaModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)

    override fun importDatabase(file: File) = nonCancellableTask("Importing Database...") {
        message1 = "Reading $file..."
        val portableDb: PortableDb = objectMapper.readValue(file, PortableDb::class.java)

        message1 = "Deleting existing database..."
        persistenceService.dropDb()   // TODO: Is it possible to hide all access to persistenceService somehow?
        runMainTask(gameService.invalidate())
        runMainTask(libraryService.invalidate())

        val libraries: Map<Int, Library> = runMainTask(libraryService.addAll(portableDb.libraries.map { it.toLibraryData() }))
            .associateBy { library -> portableDb.findLib(library.path, library.platform).id }

        runMainTask(gameService.addAll(portableDb.games.map { it.toGameRequest(libraries) }))

        doneMessage { "Imported ${portableDb.libraries.size} Libraries & ${portableDb.games.size} Games." }
    }

    override fun exportDatabase(file: File) = nonCancellableTask("Exporting Database...") {
        message1 = "Exporting Libraries:"
        message2 = libraryService.libraries.size.toString()
        val libraries = libraryService.libraries.mapWithProgress { it.toPortable() }

        message1 = "Exporting Games:"
        message2 = gameService.games.size.toString()
        val games = gameService.games.sortedBy { it.name }.mapWithProgress { it.toPortable() }

        message1 = "Writing $file..."
        message2 = ""
        file.create()
        val portableDb = PortableDb(libraries, games)
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, portableDb)

        doneMessage { "Exported ${libraryService.libraries.size} Libraries & ${gameService.games.size} Games." }
    }

    override suspend fun detectStaleData() = taskRunner.runTask("Detect Stale Data", TaskType.NonCancellable) {
        message1 = "Detecting stale data..."

        val libraries = libraryService.libraries.filterWithProgress { !it.path.isDirectory }
        val games = gameService.games.filterWithProgress { !it.path.isDirectory }

        val images = run {
            val usedImages = gameService.games.flatMapWithProgress { game ->
                game.imageUrls.screenshotUrls + listOfNotNull(game.imageUrls.thumbnailUrl, game.imageUrls.posterUrl)
            }
            imageRepository.fetchImagesExcept(usedImages)
        }

        StaleData(libraries, games, images).apply {
            doneMessage { if (isEmpty) "No stale data detected." else "Stale Data:\n${toFormattedString()}" }
        }
    }

    override suspend fun deleteStaleData(staleData: StaleData) = taskRunner.runTask("Delete Stale Data", TaskType.NonCancellable) {
        totalWork = 3

        staleData.images.let { images ->
            if (images.isNotEmpty()) {
                message1 = "Deleting stale images:"
                message2 = images.size.toString()
                imageRepository.deleteImages(images.map { it.first })
            }
        }
        incProgress()

        staleData.games.let { games ->
            if (games.isNotEmpty()) {
                runMainTask(gameService.deleteAll(games))
            }
        }
        incProgress()

        staleData.libraries.let { libraries ->
            if (staleData.libraries.isNotEmpty()) {
                runMainTask(libraryService.deleteAll(libraries))
            }
        }
        incProgress()

        doneMessage { "Deleted Stale Data:\n${staleData.toFormattedString()}" }
    }

    private fun StaleData.toFormattedString() =
        listOf(games to "Games", libraries to "Libraries", images to "Images")
            .filter { it.first.isNotEmpty() }.joinToString("\n") { "${it.first.size} ${it.second}" }

    override suspend fun deleteAllUserData() = taskRunner.runTask("Delete Game User Data") {
        message1 = "Deleting all game user data..."
        doneMessage { "All game user data deleted." }
        runMainTask(gameService.deleteAllUserData())
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
    fun toLibraryData() = LibraryData(
        name = name,
        path = path.toFile(),
        platform = Platform.valueOf(platform)
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
    is GameDataOverride.Custom -> PortableGameDataOverride.Custom(value)
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
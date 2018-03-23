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
import com.gitlab.ykrasik.gamdex.core.api.game.AddGameRequest
import com.gitlab.ykrasik.gamdex.core.api.game.GameService
import com.gitlab.ykrasik.gamdex.core.api.general.GeneralService
import com.gitlab.ykrasik.gamdex.core.api.general.StaleData
import com.gitlab.ykrasik.gamdex.core.api.library.AddLibraryRequest
import com.gitlab.ykrasik.gamdex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamdex.core.api.task.Progress
import com.gitlab.ykrasik.gamdex.core.api.task.Task
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.create
import com.gitlab.ykrasik.gamedex.util.toDateTime
import com.gitlab.ykrasik.gamedex.util.toFile
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


/**
 * User: ykrasik
 * Date: 01/04/2018
 * Time: 18:11
 */
@Singleton
class GeneralServiceImpl @Inject constructor(
    private val libraryService: LibraryService,
    private val gameService: GameService,
    private val persistenceService: PersistenceService
) : GeneralService {
    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .registerModule(JodaModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)

    // TODO: This isn't actually cancellable. Split long running tasks into cancellable and non-cancellable.
    override fun importDatabase(file: File) = Task("Importing Database...") { progress ->
        progress.message("Reading $file...")
        val portableDb = objectMapper.readValue(file, PortableDb::class.java)

        persistenceService.dropDb()
        gameService.invalidate().run()
        libraryService.invalidate().run()

        progress.message("Importing ${portableDb.libraries.size} Libraries...")
        val libraries = libraryService.addAll(portableDb.libraries.map { it.toLibraryRequest() }, progress).run()
            .associateBy { lib -> portableDb.findLib(lib.path, lib.platform).id }

        progress.message("Importing ${portableDb.games.size} Games...")
        gameService.addAll(portableDb.games.map { it.toGameRequest(libraries) }, progress).run()

        progress.doneMessage = "Imported ${portableDb.libraries.size} Libraries & ${portableDb.games.size} Games."
    }

    override fun exportDatabase(file: File) = Task("Exporting Database...") { progress ->
        progress.totalWork = libraryService.libraries.size
        progress.message("Exporting ${progress.totalWork} Libraries...")
        val libraries = libraryService.libraries.map {
            progress.inc { it.toPortable() }
        }

        progress.totalWork = gameService.games.size
        progress.message("Exporting ${progress.totalWork} Games...")
        val games = gameService.games.sortedBy { it.name }.map {
            progress.inc { it.toPortable() }
        }

        progress.message("Writing $file...")
        progress.totalWork = 1
        val portableDb = PortableDb(libraries, games)
        file.create()
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, portableDb)
        progress.inc()

        progress.doneMessage = "Exported ${libraryService.libraries.size} Libraries & ${gameService.games.size} Games."
    }

    override fun detectStaleData() = Task("Detecting stale data...") { progress ->
        val libraries = detectStaleData("libraries", progress) {
            progress.totalWork = libraryService.libraries.size
            libraryService.libraries.filter { library ->
                progress.inc { !library.path.isDirectory }
            }
        }

        val games = detectStaleData("games", progress) {
            progress.totalWork = gameService.games.size
            gameService.games.filter { game ->
                progress.inc { !game.path.isDirectory }
            }
        }

        val images = detectStaleData("images", progress) {
            progress.totalWork = gameService.games.size
            val usedImages = gameService.games.flatMap { game ->
                progress.inc {
                    game.imageUrls.screenshotUrls + listOf(game.imageUrls.thumbnailUrl, game.imageUrls.posterUrl).mapNotNull { it }
                }
            }
            persistenceService.fetchImagesExcept(usedImages)    // FIXME: Go through an image repository!
        }

        StaleData(libraries, games, images).apply {
            progress.doneMessage = if (isEmpty) "No stale data detected." else "Stale Data:\n${toFormattedString()}"
        }
    }

    private suspend fun <T> detectStaleData(name: String, progress: Progress, f: suspend () -> List<T>): List<T> {
        progress.message("Detecting stale $name...")
        val staleData = f()
        progress.message("Detected ${staleData.size} stale $name.")
        return staleData
    }

    override fun deleteStaleData(staleData: StaleData) = Task("Deleting stale data...") { progress ->
        progress.totalWork = 3

        if (staleData.images.isNotEmpty()) {
            progress.message("Deleting stale images...")
            persistenceService.deleteImages(staleData.images.map { it.first })   // FIXME: Go through an image repository!
        }
        progress.inc()

        if (staleData.games.isNotEmpty()) {
            progress.message("Deleting stale games...")
            gameService.deleteAll(staleData.games).run()
        }
        progress.inc()

        if (staleData.libraries.isNotEmpty()) {
            progress.message("Deleting stale libraries...")
            libraryService.deleteAll(staleData.libraries).run()
        }
        progress.inc()

        progress.doneMessage = "Deleted Stale Data:\n${staleData.toFormattedString()}"
    }

    private fun StaleData.toFormattedString() =
        listOf(games to "Games", libraries to "Libraries", images to "Images")
            .filter { it.first.isNotEmpty() }.joinToString("\n") { "${it.first.size} ${it.second}" }

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
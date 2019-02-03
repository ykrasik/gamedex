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

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.game.AddGameRequest
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.toDateTime
import com.gitlab.ykrasik.gamedex.util.toFile
import java.io.File

/**
 * User: ykrasik
 * Date: 03/11/2018
 * Time: 12:37
 */
internal data class PortableDb(
    val libraries: List<PortableLibrary>,
    val games: List<PortableGame>
) {
    fun findLib(path: File, type: LibraryType, platform: Platform?) = libraries.find { lib ->
        lib.path == path.path &&
            lib.type == type.name &&
            lib.platform == platform?.name
    }!!
}

internal data class PortableLibrary(
    val id: Int,
    val path: String,
    val type: String,
    val platform: String?,
    val name: String
) {
    fun toLibraryData() = LibraryData(
        name = name,
        path = path.toFile(),
        type = LibraryType.valueOf(type),
        platform = platform?.let(Platform::valueOf)
    )
}

internal fun Library.toPortable() = PortableLibrary(
    id = id,
    path = path.toString(),
    type = type.name,
    platform = platformOrNull?.name,
    name = name
)

internal data class PortableGame(
    val libraryId: Int,
    val path: String,
    val createDate: Long,
    val updateDate: Long,
    val providerData: List<PortableProviderData>,
    val userData: PortableUserData?
) {
    fun toGameRequest(libraries: Map<Int, Library>) = AddGameRequest(
        metadata = Metadata(
            libraryId = libraries.getOrElse(libraryId) { throw IllegalArgumentException("Invalid library id: $libraryId") }.id,
            path = path,
            timestamp = Timestamp(
                createDate = createDate.toDateTime(),
                updateDate = updateDate.toDateTime()
            )
        ),
        providerData = providerData.map { it.toProviderData() },
        userData = userData?.toUserData() ?: UserData.Null
    )
}

internal fun Game.toPortable() = PortableGame(
    libraryId = library.id,
    path = rawGame.metadata.path,
    createDate = createDate.millis,
    updateDate = updateDate.millis,
    providerData = rawGame.providerData.map { it.toPortable() },
    userData = userData.takeIf { it != UserData.Null }?.toPortable()
)

internal data class PortableProviderData(
    val providerId: ProviderId,
    val createDate: Long,
    val updateDate: Long,
    val apiUrl: String,
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
    val screenshotUrls: List<String>,
    val siteUrl: String
) {
    fun toProviderData() = ProviderData(
        header = ProviderHeader(
            id = providerId,
            apiUrl = apiUrl
        ),
        gameData = GameData(
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
        ),
        siteUrl = siteUrl,
        timestamp = Timestamp(
            createDate = createDate.toDateTime(),
            updateDate = updateDate.toDateTime()
        )
    )

    private fun toScore(score: Double?, numReviews: Int?) = score?.let { Score(it, numReviews!!) }
}

internal fun ProviderData.toPortable() = PortableProviderData(
    providerId = header.id,
    createDate = timestamp.createDate.millis,
    updateDate = timestamp.updateDate.millis,
    apiUrl = header.apiUrl,
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
    screenshotUrls = gameData.imageUrls.screenshotUrls,
    siteUrl = siteUrl
)

internal data class PortableUserData(
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
        nameOverride?.toOverride()?.let { overrides += GameDataType.Name to it }
        descriptionOverride?.toOverride()?.let { overrides += GameDataType.Description to it }
        releaseDateOverride?.toOverride()?.let { overrides += GameDataType.ReleaseDate to it }
        criticScoreOverride?.toOverride()?.let { overrides += GameDataType.CriticScore to it }
        userScoreOverride?.toOverride()?.let { overrides += GameDataType.UserScore to it }
        genresOverride?.toOverride()?.let { overrides += GameDataType.Genres to it }
        thumbnailOverride?.toOverride()?.let { overrides += GameDataType.Thumbnail to it }
        posterOverride?.toOverride()?.let { overrides += GameDataType.Poster to it }
        screenshotsOverride?.toOverride()?.let { overrides += GameDataType.Screenshots to it }

        return UserData(
            overrides = overrides,
            tags = tags,
            excludedProviders = excludedProviders
        )
    }
}

internal fun UserData.toPortable() = PortableUserData(
    nameOverride = overrides[GameDataType.Name]?.toPortable(),
    descriptionOverride = overrides[GameDataType.Description]?.toPortable(),
    releaseDateOverride = overrides[GameDataType.ReleaseDate]?.toPortable(),
    criticScoreOverride = overrides[GameDataType.CriticScore]?.toPortable(),
    userScoreOverride = overrides[GameDataType.UserScore]?.toPortable(),
    genresOverride = overrides[GameDataType.Genres]?.toPortable(),
    thumbnailOverride = overrides[GameDataType.Thumbnail]?.toPortable(),
    posterOverride = overrides[GameDataType.Poster]?.toPortable(),
    screenshotsOverride = overrides[GameDataType.Screenshots]?.toPortable(),
    tags = tags,
    excludedProviders = excludedProviders
)

internal fun GameDataOverride.toPortable() = when (this) {
    is GameDataOverride.Provider -> PortableGameDataOverride.Provider(provider)
    is GameDataOverride.Custom -> PortableGameDataOverride.Custom(value)
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = PortableGameDataOverride.Provider::class, name = "provider"),
    JsonSubTypes.Type(value = PortableGameDataOverride.Custom::class, name = "custom")
)
internal sealed class PortableGameDataOverride {
    abstract fun toOverride(): GameDataOverride

    data class Provider(val provider: ProviderId) : PortableGameDataOverride() {
        override fun toOverride() = GameDataOverride.Provider(provider)
    }

    data class Custom(val data: Any) : PortableGameDataOverride() {
        override fun toOverride() = GameDataOverride.Custom(data)
    }
}
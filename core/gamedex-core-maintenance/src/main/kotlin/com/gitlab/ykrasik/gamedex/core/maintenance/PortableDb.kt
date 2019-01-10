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
    fun findLib(path: File, platform: Platform) = libraries.find { it.path == path.path && it.platform == platform.name }!!
}

internal data class PortableLibrary(
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

internal fun Library.toPortable() = PortableLibrary(
    id = id,
    path = path.toString(),
    platform = platform.name,
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

internal fun UserData.toPortable() = PortableUserData(
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
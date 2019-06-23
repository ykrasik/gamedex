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
import com.gitlab.ykrasik.gamedex.util.dateTime
import com.gitlab.ykrasik.gamedex.util.file
import java.io.File
import com.gitlab.ykrasik.gamedex.Game as DomainGame
import com.gitlab.ykrasik.gamedex.GameDataOverride as DomainGameDataOverride
import com.gitlab.ykrasik.gamedex.Library as DomainLibrary
import com.gitlab.ykrasik.gamedex.ProviderData as DomainProviderData
import com.gitlab.ykrasik.gamedex.UserData as DomainUserData

/**
 * User: ykrasik
 * Date: 03/11/2018
 * Time: 12:37
 */
internal object PortableDb {
    data class Db(
        val libraries: List<Library>,
        val games: List<Game>
    ) {
        fun findLib(path: File, type: LibraryType, platform: Platform?) = libraries.find { lib ->
            lib.path == path.path &&
                lib.type == type.name &&
                lib.platform == platform?.name
        }!!
    }

    data class Library(
        val id: Int,
        val path: String,
        val type: String,
        val platform: String?,
        val name: String
    ) {
        fun toLibraryData() = LibraryData(
            name = name,
            path = path.file,
            type = LibraryType.valueOf(type),
            platform = platform?.let(Platform::valueOf)
        )
    }

    fun DomainLibrary.toPortable() = Library(
        id = id,
        path = path.toString(),
        type = type.name,
        platform = platformOrNull?.name,
        name = name
    )

    data class Game(
        val libraryId: Int,
        val path: String,
        val providerData: List<ProviderData>,
        val userData: UserData?,
        val createDate: Long,
        val updateDate: Long
    ) {
        fun toGameRequest(libraries: Map<Int, DomainLibrary>) = AddGameRequest(
            metadata = Metadata(
                libraryId = libraries.getOrElse(libraryId) { throw IllegalArgumentException("Invalid library id: $libraryId") }.id,
                path = path,
                timestamp = Timestamp(
                    createDate = createDate.dateTime,
                    updateDate = updateDate.dateTime
                )
            ),
            providerData = providerData.map { it.toProviderData() },
            userData = userData?.toUserData() ?: DomainUserData.Null
        )
    }

    fun DomainGame.toPortable() = Game(
        libraryId = library.id,
        path = metadata.path,
        createDate = createDate.millis,
        updateDate = updateDate.millis,
        providerData = providerData.map { it.toPortable() },
        userData = userData.takeIf { it != DomainUserData.Null }?.toPortable()
    )

    data class ProviderData(
        val providerId: ProviderId,
        val providerGameId: String,
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
        val siteUrl: String,
        val createDate: Long,
        val updateDate: Long
    ) {
        fun toProviderData() = DomainProviderData(
            header = ProviderHeader(
                providerId = providerId,
                providerGameId = providerGameId
            ),
            gameData = GameData(
                name = name,
                description = description,
                releaseDate = releaseDate,
                criticScore = toScore(criticScore, numCriticReviews),
                userScore = toScore(userScore, numUserReviews),
                genres = genres,
                thumbnailUrl = thumbnailUrl,
                posterUrl = posterUrl,
                screenshotUrls = screenshotUrls
            ),
            siteUrl = siteUrl,
            timestamp = Timestamp(
                createDate = createDate.dateTime,
                updateDate = updateDate.dateTime
            )
        )

        private fun toScore(score: Double?, numReviews: Int?) = score?.let { Score(it, numReviews!!) }
    }

    private fun DomainProviderData.toPortable() = ProviderData(
        providerId = providerId,
        providerGameId = providerGameId,
        name = gameData.name,
        description = gameData.description,
        releaseDate = gameData.releaseDate,
        criticScore = gameData.criticScore?.score,
        numCriticReviews = gameData.criticScore?.numReviews,
        userScore = gameData.userScore?.score,
        numUserReviews = gameData.userScore?.numReviews,
        genres = gameData.genres,
        thumbnailUrl = gameData.thumbnailUrl,
        posterUrl = gameData.posterUrl,
        screenshotUrls = gameData.screenshotUrls,
        siteUrl = siteUrl,
        createDate = timestamp.createDate.millis,
        updateDate = timestamp.updateDate.millis
    )

    data class UserData(
        val nameOverride: GameDataOverride?,
        val descriptionOverride: GameDataOverride?,
        val releaseDateOverride: GameDataOverride?,
        val criticScoreOverride: GameDataOverride?,
        val userScoreOverride: GameDataOverride?,
        val genresOverride: GameDataOverride?,
        val thumbnailOverride: GameDataOverride?,
        val posterOverride: GameDataOverride?,
        val screenshotsOverride: GameDataOverride?,
        val tags: List<String>,
        val excludedProviders: List<ProviderId>
    ) {
        fun toUserData(): DomainUserData {
            val overrides = mutableMapOf<GameDataType, DomainGameDataOverride>()
            nameOverride?.toOverride()?.let { overrides += GameDataType.Name to it }
            descriptionOverride?.toOverride()?.let { overrides += GameDataType.Description to it }
            releaseDateOverride?.toOverride()?.let { overrides += GameDataType.ReleaseDate to it }
            criticScoreOverride?.toOverride()?.let { overrides += GameDataType.CriticScore to it }
            userScoreOverride?.toOverride()?.let { overrides += GameDataType.UserScore to it }
            genresOverride?.toOverride()?.let { overrides += GameDataType.Genres to it }
            thumbnailOverride?.toOverride()?.let { overrides += GameDataType.Thumbnail to it }
            posterOverride?.toOverride()?.let { overrides += GameDataType.Poster to it }
            screenshotsOverride?.toOverride()?.let { overrides += GameDataType.Screenshots to it }

            return DomainUserData(
                overrides = overrides,
                tags = tags,
                excludedProviders = excludedProviders
            )
        }
    }

    private fun DomainUserData.toPortable() = UserData(
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

    private fun DomainGameDataOverride.toPortable() = when (this) {
        is DomainGameDataOverride.Provider -> GameDataOverride.Provider(provider)
        is DomainGameDataOverride.Custom -> GameDataOverride.Custom(value)
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = GameDataOverride.Provider::class, name = "provider"),
        JsonSubTypes.Type(value = GameDataOverride.Custom::class, name = "custom")
    )
    sealed class GameDataOverride {
        abstract fun toOverride(): DomainGameDataOverride

        data class Provider(val provider: ProviderId) : GameDataOverride() {
            override fun toOverride() = DomainGameDataOverride.Provider(provider)
        }

        data class Custom(val data: Any) : GameDataOverride() {
            override fun toOverride() = DomainGameDataOverride.Custom(data)
        }
    }
}
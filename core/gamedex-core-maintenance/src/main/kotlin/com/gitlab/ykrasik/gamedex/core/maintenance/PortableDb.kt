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

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.game.AddGameRequest
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.dateTime
import com.gitlab.ykrasik.gamedex.util.file
import com.gitlab.ykrasik.gamedex.util.now
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
object PortableDb {
    data class Db(
        val libraries: List<Library> = emptyList(),
        val games: List<Game> = emptyList()
    ) {
        fun findLib(path: File, type: LibraryType, platform: Platform?) = libraries.find { lib ->
            lib.path == path.path &&
                lib.type == type.name &&
                lib.platform == platform
        }!!
    }

    data class Library(
        val id: Int,
        val path: String,
        val type: String,
        val platform: Platform?,
        val name: String
    ) {
        fun toLibraryData() = LibraryData(
            name = name,
            path = path.file,
            type = LibraryType.valueOf(type),
            platform = platform
        )
    }

    fun DomainLibrary.toPortable() = Library(
        id = id,
        path = path.toString(),
        type = type.name,
        platform = platformOrNull,
        name = name
    )

    data class Game(
        val libraryId: Int,
        val path: String,
        val providerData: List<ProviderData> = emptyList(),
        val userData: UserData? = null,
        val createDate: Long = now.millis,
        val updateDate: Long = now.millis
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
        val description: String? = null,
        val releaseDate: String? = null,
        val criticScore: Double? = null,
        val numCriticReviews: Int? = null,
        val userScore: Double? = null,
        val numUserReviews: Int? = null,
        val genres: List<String> = emptyList(),
        val thumbnailUrl: String? = null,
        val posterUrl: String? = null,
        val screenshotUrls: List<String> = emptyList(),
        val siteUrl: String,
        val createDate: Long = now.millis,
        val updateDate: Long = now.millis
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
        val nameOverride: GameDataOverride? = null,
        val descriptionOverride: GameDataOverride? = null,
        val releaseDateOverride: GameDataOverride? = null,
        val criticScoreOverride: GameDataOverride? = null,
        val userScoreOverride: GameDataOverride? = null,
        val genresOverride: GameDataOverride? = null,
        val thumbnailOverride: GameDataOverride? = null,
        val posterOverride: GameDataOverride? = null,
        val screenshotsOverride: GameDataOverride? = null,
        val tags: List<String> = emptyList(),
        val excludedProviders: List<ProviderId> = emptyList()
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
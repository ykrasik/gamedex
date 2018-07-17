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

package com.gitlab.ykrasik.gamedex

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import org.joda.time.DateTime

/**
 * User: ykrasik
 * Date: 26/05/2016
 * Time: 15:32
 */
data class Game(
    val rawGame: RawGame,
    val library: Library,
    val gameData: GameData,
    val folderMetadata: FolderMetadata  // TODO: Is this the best place to put this?
) {
    val id get() = rawGame.id
    val path = library.path.resolve(metadata.path)
    val createDate get() = metadata.createDate
    val updateDate get() = metadata.updateDate
    val userData get() = rawGame.userData
    private val metadata get() = rawGame.metadata

    val platform get() = library.platform

    val name get() = gameData.name
    val description get() = gameData.description
    val releaseDate get() = gameData.releaseDate
    val criticScore get() = gameData.criticScore
    val userScore get() = gameData.userScore
    val minScore get() = withBothScores { c, u -> minOf(c, u) }
    val avgScore get() = withBothScores { c, u -> (c + u) / 2 }
    private inline fun withBothScores(f: (criticScore: Double, userScore: Double) -> Double): Double? =
        criticScore?.let { c -> userScore?.let { u -> f(c.score, u.score) } }

    val genres get() = gameData.genres

    val imageUrls get() = gameData.imageUrls
    val thumbnailUrl get() = imageUrls.thumbnailUrl
    val posterUrl get() = imageUrls.posterUrl
    val screenshotUrls get() = imageUrls.screenshotUrls

    val tags get() = rawGame.userData?.tags ?: emptyList()

    val providerHeaders get() = rawGame.providerData.map { it.header }
    val existingProviders get() = providerHeaders.map { it.id }
    val excludedProviders get() = userData?.excludedProviders ?: emptyList()

    override fun toString() = "[$platform] Game(id = $id, name = '$name', path = $path)"
}

typealias GameId = Int

data class RawGame(
    val id: GameId,
    val metadata: Metadata,
    val providerData: List<ProviderData>,
    val userData: UserData?    // TODO: Make this non-nullable?
) {
    fun withMetadata(metadata: Metadata) = copy(metadata = metadata)
    fun withMetadata(f: (Metadata) -> Metadata) = withMetadata(f(metadata))
}

data class ProviderData(
    val header: ProviderHeader,
    val gameData: GameData
) {
    fun withCreateDate(createDate: DateTime) = copy(header = header.withCreateDate(createDate))
}

@JsonIgnoreProperties("thumbnailUrl", "posterUrl", "screenshotUrls")
data class GameData(
    val siteUrl: String,
    val name: String,
    val description: String?,
    val releaseDate: String?,
    val criticScore: Score?,
    val userScore: Score?,
    val genres: List<String>,
    val imageUrls: ImageUrls
) {
    val thumbnailUrl get() = imageUrls.thumbnailUrl
    val posterUrl get() = imageUrls.posterUrl
    val screenshotUrls get() = imageUrls.screenshotUrls
}

data class Score(
    val score: Double,
    val numReviews: Int
) : Comparable<Score> {
    override fun compareTo(other: Score) = score.compareTo(other.score)
}

@JsonIgnoreProperties("createDate", "updateDate")
data class ProviderHeader(
    val id: ProviderId,
    val apiUrl: String,
    val timestamp: Timestamp
) {
    val createDate get() = timestamp.createDate
    val updateDate get() = timestamp.updateDate
    fun withCreateDate(createDate: DateTime) = copy(timestamp = timestamp.withCreateDate(createDate))
}

data class ImageUrls(
    val thumbnailUrl: String?,
    val posterUrl: String?,
    val screenshotUrls: List<String>
)

@JsonIgnoreProperties("createDate", "updateDate")
data class Metadata(
    val libraryId: Int,
    val path: String,
    val timestamp: Timestamp
) {
    val createDate get() = timestamp.createDate
    val updateDate get() = timestamp.updateDate
    fun withCreateDate(createDate: DateTime) = copy(timestamp = timestamp.withCreateDate(createDate))
    fun updatedNow() = copy(timestamp = timestamp.updatedNow())
}

data class Timestamp(
    val createDate: DateTime,
    val updateDate: DateTime
) {
    fun withCreateDate(createDate: DateTime) = copy(createDate = createDate)
    fun updatedNow() = copy(updateDate = com.gitlab.ykrasik.gamedex.util.now)

    companion object {
        val now get() = com.gitlab.ykrasik.gamedex.util.now.let { now -> Timestamp(now, now) }
    }
}

data class FolderMetadata(
    val rawName: String,
    val gameName: String,
    val order: Int?,            // TODO: Consider adding option to display this.
    val metaTag: String?,
    val version: String?
)

enum class GameDataType(val displayName: String) {
    name_("Name"),
    description("Description"),
    releaseDate("Release Date"),
    criticScore("Critic Score"),
    userScore("User Score"),
    genres("Genres"),
    thumbnail("Thumbnail"),
    poster("Poster"),
    screenshots("Screenshots")
}

data class UserData(
    val overrides: Map<GameDataType, GameDataOverride> = emptyMap(),
    val tags: List<String> = emptyList(),
    val excludedProviders: List<ProviderId> = emptyList()
) {
    fun nameOverride() = overrides[GameDataType.name_]
    fun descriptionOverride() = overrides[GameDataType.description]
    fun releaseDateOverride() = overrides[GameDataType.releaseDate]
    fun criticScoreOverride() = overrides[GameDataType.criticScore]
    fun userScoreOverride() = overrides[GameDataType.userScore]
    fun genresOverride() = overrides[GameDataType.genres]
    fun thumbnailOverride() = overrides[GameDataType.thumbnail]
    fun posterOverride() = overrides[GameDataType.poster]
    fun screenshotsOverride() = overrides[GameDataType.screenshots]

    fun merge(other: UserData): UserData = UserData(
        overrides = this.overrides + other.overrides,
        tags = (this.tags + other.tags).distinct(),
        excludedProviders = (this.excludedProviders + other.excludedProviders).distinct()
    )
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = GameDataOverride.Provider::class, name = "provider"),
    JsonSubTypes.Type(value = GameDataOverride.Custom::class, name = "custom")
)
sealed class GameDataOverride {
    data class Provider(val provider: ProviderId) : GameDataOverride()
    data class Custom(val value: Any) : GameDataOverride()
}
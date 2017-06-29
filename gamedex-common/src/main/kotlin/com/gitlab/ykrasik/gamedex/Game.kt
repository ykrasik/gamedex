package com.gitlab.ykrasik.gamedex

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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
    val providerHeaders: List<ProviderHeader>,
    val imageUrls: ImageUrls,
    val folderMetaData: FolderMetaData  // TODO: Is this the best place to put this?
) {
    val id get() = rawGame.id
    val path = library.path.resolve(metaData.path)
    val updateDate get() = metaData.updateDate
    val userData get() = rawGame.userData
    private val metaData get() = rawGame.metaData

    val platform get() = library.platform

    val name get() = gameData.name
    val description get() = gameData.description
    val releaseDate get() = gameData.releaseDate
    val criticScore get() = gameData.criticScore
    val userScore get() = gameData.userScore
    val minScore get() = criticScore?.let { c -> userScore?.let { u -> minOf(c.score, u.score) } }
    val avgScore get() = criticScore?.let { c -> userScore?.let { u -> (c.score + u.score) / 2 } }
    val genres get() = gameData.genres

    val thumbnailUrl get() = imageUrls.thumbnailUrl
    val posterUrl get() = imageUrls.posterUrl
    val screenshotUrls get() = imageUrls.screenshotUrls

    val tags get() = rawGame.userData?.tags ?: emptyList()

    val existingProviders get() = rawGame.providerData.map { it.header.id }
    val excludedProviders get() = userData?.excludedProviders ?: emptyList()

    override fun toString() = "[$platform] Game(id = $id, name = '$name', path = $path)"
}

data class RawGame(
    val id: Int,
    val metaData: MetaData,
    val providerData: List<ProviderData>,
    val userData: UserData?    // TODO: Make this non-nullable
) {
    fun withMetadata(metaData: MetaData) = copy(metaData = metaData)
    fun withMetadata(f: (MetaData) -> MetaData) = withMetadata(f(metaData))
}

data class ProviderData(
    val header: ProviderHeader,
    val gameData: GameData,
    val imageUrls: ImageUrls
)

data class GameData(
    val siteUrl: String,
    val name: String,
    val description: String?,
    val releaseDate: String?,
    val criticScore: Score?,
    val userScore: Score?,
    val genres: List<String>
)

data class Score(
    val score: Double,
    val numReviews: Int
) : Comparable<Score> {
    override fun compareTo(other: Score) = score.compareTo(other.score)
}

data class ProviderHeader(
    val id: ProviderId,
    val apiUrl: String,
    val updateDate: DateTime
)

data class ImageUrls(
    val thumbnailUrl: String?,
    val posterUrl: String?,
    val screenshotUrls: List<String>
)

// TODO: Rename to Metadata
data class MetaData(
    val libraryId: Int,
    val path: String,
    val updateDate: DateTime
)

data class FolderMetaData(
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
    data class Custom(val data: Any) : GameDataOverride()
}
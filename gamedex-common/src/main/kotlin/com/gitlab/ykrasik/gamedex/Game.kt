package com.gitlab.ykrasik.gamedex

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitlab.ykrasik.gamedex.util.now
import org.joda.time.DateTime
import java.io.File
import java.nio.file.Paths

/**
 * User: ykrasik
 * Date: 26/05/2016
 * Time: 15:32
 */
data class Game(
    val rawGame: RawGame,
    val library: Library,
    private val gameData: GameData,
    val providerHeaders: List<ProviderHeader>,
    val imageUrls: ImageUrls
) {
    val id get() = rawGame.id
    val path: File = Paths.get(library.path.toString(), metaData.path.toString()).toFile()
    val updateDate get() = metaData.updateDate
    val userData get() = rawGame.userData
    private val metaData get() = rawGame.metaData

    val platform get() = library.platform

    val name get() = gameData.name
    val description get() = gameData.description
    val releaseDate get() = gameData.releaseDate
    val criticScore get() = gameData.criticScore
    val userScore get() = gameData.userScore
    val genres get() = gameData.genres

    val thumbnailUrl get() = imageUrls.thumbnailUrl
    val posterUrl get() = imageUrls.posterUrl
    val screenshotUrls get() = imageUrls.screenshotUrls

    val tags get() = rawGame.userData?.tags ?: emptyList()

    override fun toString() = "[$platform] Game(id = $id, name = '$name', path = $path)"
}

data class RawGame(
    val id: Int,
    val metaData: MetaData,
    val providerData: List<ProviderData>,
    val userData: UserData?    // TODO: Make this non-nullable
) {
    // TODO: Consider moving this responsibility to the persistence layer.
    fun updatedNow() = copy(metaData = metaData.updatedNow())
}

data class ProviderData(
    val header: ProviderHeader,
    val gameData: GameData,
    val imageUrls: ImageUrls
)

data class GameData(
    val updateDate: DateTime,
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
    val type: GameProviderType,
    val apiUrl: String
)

data class ImageUrls(
    val thumbnailUrl: String?,
    val posterUrl: String?,
    val screenshotUrls: List<String>
)

data class MetaData(
    val libraryId: Int,
    val path: File,
    val updateDate: DateTime
) {
    // TODO: Consider moving this responsibility to the persistence layer.
    fun updatedNow() = copy(updateDate = now)
}

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
    val excludedProviders: List<GameProviderType> = emptyList()
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
    data class Provider(val provider: GameProviderType) : GameDataOverride()
    data class Custom(val data: Any) : GameDataOverride()
}
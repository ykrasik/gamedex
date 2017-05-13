package com.gitlab.ykrasik.gamedex

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.joda.time.DateTime
import java.io.File

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
    val path get() = metaData.path
    val lastModified get() = metaData.lastModified
    val userData get() = rawGame.userData
    val metaData get() = rawGame.metaData

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

    override fun toString() = "[$platform] Game(id = $id, name = '$name', path = $path)"
}

data class RawGame(
    val id: Int,
    val metaData: MetaData,
    val providerData: List<ProviderData>,
    val userData: UserData?
)

data class ProviderData(
    val header: ProviderHeader,
    val gameData: GameData,
    val imageUrls: ImageUrls
)

data class GameData(
    val name: String,
    val description: String?,
    val releaseDate: String?,
    val criticScore: Double?,
    val userScore: Double?,
    val genres: List<String>
)

data class ProviderHeader(
    val type: GameProviderType,
    val apiUrl: String,
    val siteUrl: String
)

data class ImageUrls(
    val thumbnailUrl: String?,
    val posterUrl: String?,
    val screenshotUrls: List<String>
)

data class MetaData(
    val libraryId: Int,
    val path: File,
    val lastModified: DateTime
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
    val overrides: Map<GameDataType, GameDataOverride> = emptyMap()
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
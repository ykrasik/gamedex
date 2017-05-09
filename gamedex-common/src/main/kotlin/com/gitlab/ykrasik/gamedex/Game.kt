package com.gitlab.ykrasik.gamedex

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.joda.time.DateTime
import org.joda.time.LocalDate
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
    val providerData: List<ProviderData>,
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
    val rawGameData: List<RawGameData>,
    val userData: UserData?
)

// TODO: Need a better name for this... providerData?
data class RawGameData(
    val providerData: ProviderData,
    val gameData: GameData,
    val imageUrls: ImageUrls
)

data class GameData(
    val name: String,
    val description: String?,
    val releaseDate: LocalDate?,
    val criticScore: Double?,
    val userScore: Double?,
    val genres: List<String>
)

// TODO: Maybe rename to providerHeader?
data class ProviderData(
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

data class UserData(
    val overrides: GameDataOverrides = GameDataOverrides()
)

data class GameDataOverrides(
    val name: GameDataOverride? = null,
    val description: GameDataOverride? = null,
    val releaseDate: GameDataOverride? = null,
    val criticScore: GameDataOverride? = null,
    val userScore: GameDataOverride? = null,
    val genres: GameDataOverride? = null,
    val thumbnail: GameDataOverride? = null,
    val poster: GameDataOverride? = null,
    val screenshots: GameDataOverride? = null
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = GameDataOverride.Provider::class, name = "provider"),
    JsonSubTypes.Type(value = GameDataOverride.Custom::class, name = "custom")
)
sealed class GameDataOverride {
    data class Provider(val provider: GameProviderType) : GameDataOverride()
    data class Custom(val data: Any) : GameDataOverride()
}
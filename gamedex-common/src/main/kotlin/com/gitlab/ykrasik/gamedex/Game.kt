package com.gitlab.ykrasik.gamedex

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
    private val library: Library,
    private val gameData: GameData,
    val providerData: List<ProviderData>,
    private val imageUrls: ImageUrls
) {
    val id get() = rawGame.id
    val path get() = metaData.path
    val lastModified get() = metaData.lastModified
    val priorityOverride get() = rawGame.priorityOverride
    private val metaData get() = rawGame.metaData

    val libraryId get() = library.id
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

    override fun toString() = "[$id] Game(name = '$name')"
}

data class RawGame(
    val id: Int,
    val metaData: MetaData,
    val rawGameData: List<RawGameData>,
    val priorityOverride: ProviderPriorityOverride?
)

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

data class ProviderPriorityOverride(
    val name: GameProviderType?,
    val description: GameProviderType?,
    val releaseDate: GameProviderType?,
    val criticScore: GameProviderType?,
    val userScore: GameProviderType?,
    // TODO: Consider adding priority overrides for genres as well.
    val thumbnail: GameProviderType?,
    val poster: GameProviderType?,
    val screenshots: GameProviderType?
    // TODO: This makes all screenshots come from a single provider. If needed, add more flexibility in the future - can use urls instead of Type.
)
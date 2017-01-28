package com.github.ykrasik.gamedex.common.datamodel

import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.io.File

/**
 * User: ykrasik
 * Date: 26/05/2016
 * Time: 15:32
 */
data class Game(
    val id: Int,
    private val metaData: GameMetaData,
    private val gameData: GameData,
    val providerData: List<GameProviderData>
) {
    val libraryId: Int get() = metaData.libraryId
    val path: File get() = metaData.path
    val lastModified: DateTime get() = metaData.lastModified

    val name: String get() = gameData.name
    val description: String? get() = gameData.description
    val releaseDate: LocalDate? get() = gameData.releaseDate

    val criticScore: Double? get() = gameData.criticScore
    val userScore: Double? get() = gameData.userScore

    val genres: List<String> get() = gameData.genres

    override fun toString() = "Game(id = $id, name = $name, path = $path)"
}

data class GameMetaData(
    val libraryId: Int,
    val path: File,
    val lastModified: DateTime
)

data class GameData(
    val name: String,
    val description: String?,
    val releaseDate: LocalDate?,

    val criticScore: Double?,
    val userScore: Double?,

    val genres: List<String>
)

data class GameProviderData(
    val type: DataProviderType,
    val detailUrl: String
)

data class GameImageData(
    val thumbnailUrl: String?,
    val posterUrl: String?,
    val screenshot1Url: String?,
    val screenshot2Url: String?,
    val screenshot3Url: String?,
    val screenshot4Url: String?,
    val screenshot5Url: String?,
    val screenshot6Url: String?,
    val screenshot7Url: String?,
    val screenshot8Url: String?,
    val screenshot9Url: String?,
    val screenshot10Url: String?
)
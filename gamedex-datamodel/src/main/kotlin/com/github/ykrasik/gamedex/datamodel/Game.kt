package com.github.ykrasik.gamedex.datamodel

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
    val path: File,
    val lastModified: DateTime,
    val libraryId: Int,
    val data: GameData
) {
    val name: String get() = data.name
    val description: String? get() = data.description
    val releaseDate: LocalDate? get() = data.releaseDate

    val criticScore: Double? get() = data.criticScore
    val userScore: Double? get() = data.userScore

    val genres: List<String> get() = data.genres

    val providerData: List<ProviderData> get() = data.providerData

    override fun toString() = "Game(id = $id, name = $name, path = $path)"
}

data class GameData(
    val name: String,
    val description: String?,
    val releaseDate: LocalDate?,
    val criticScore: Double?,
    val userScore: Double?,
    val genres: List<String>,
    val providerData: List<ProviderData>
)

data class ProviderData(
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
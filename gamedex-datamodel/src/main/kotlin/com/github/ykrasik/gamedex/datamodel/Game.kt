package com.github.ykrasik.gamedex.datamodel

import com.github.ykrasik.gamedex.common.Id
import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.nio.file.Path

/**
 * User: ykrasik
 * Date: 26/05/2016
 * Time: 15:32
 */
data class GameBasicData(
    val name: String,
    val description: String?,
    val releaseDate: LocalDate?
)

data class GameScoreData(
    val criticScore: Double?,
    val userScore: Double?
)

data class GameImageData(
    val thumbnail: ImageData,
    val poster: ImageData
)

data class Game(
    val id: Id<Game>,
    val path: Path,
    val lastModified: DateTime,
    val library: Library,  // TODO: Is this needed? It holds no interesting information, can probably save just the Id.
    val data: GameData
) {
    val name: String get() = data.basicData.name
    val description: String? get() = data.basicData.description
    val releaseDate: LocalDate? get() = data.basicData.releaseDate

    val criticScore: Double? get() = data.scoreData.criticScore
    val userScore: Double? get() = data.scoreData.userScore

    val thumbnail: ImageData? get() = data.imageData.thumbnail
    val poster: ImageData? get() = data.imageData.poster

    val genres: List<Genre> get() = data.genres

    val providerData: List<ProviderData> get() = data.providerData

    override fun toString() = "Game(id = $id, name = ${data.basicData.name}, path = $path)"
}

data class GameData(
    val basicData: GameBasicData,
    val scoreData: GameScoreData,
    val imageData: GameImageData,
    val genres: List<Genre>,
    val providerData: List<ProviderData>
)

data class GameDataDto(
    val basicData: GameBasicData,
    val scoreData: GameScoreData,
    val imageData: GameImageData,
    val genresNames: List<String>,
    val providerData: List<ProviderData>
)

data class ProviderData(
    val type: DataProviderType,
    val detailUrl: String
)
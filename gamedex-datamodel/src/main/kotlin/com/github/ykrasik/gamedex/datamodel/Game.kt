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
data class Game(
    val id: Id<Game>,
    val lastModified: DateTime,
    val library: Library,  // TODO: Is this needed? It holds no interesting information, can probably save just the Id.
    val data: GameData
) {
    val name: String get() = data.name
    val path: Path get() = data.path
    val description: String? get() = data.description
    val releaseDate: LocalDate? get() = data.releaseDate
    val criticScore: Double? get() = data.criticScore
    val userScore: Double? get() = data.userScore
    val thumbnail: ImageData? get() = data.thumbnail
    val poster: ImageData? get() = data.poster
    val genres: List<Genre> get() = data.genres
    val providerSpecificData: List<GameProviderSpecificData> get() = data.providerSpecificData

    override fun toString() = "Game(id = $id, name = $name, path = $path)"
}

data class GameData(
    val path: Path,
    val name: String,
    val description: String?,
    val releaseDate: LocalDate?,

    val criticScore: Double?,
    val userScore: Double?,

    val thumbnail: ImageData?,
    val poster: ImageData?,

    val genres: List<Genre>,

    val providerSpecificData: List<GameProviderSpecificData>
)

interface GameProviderSpecificData {

}
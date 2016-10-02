package com.gitlab.ykrasik.gamedex.persistence.entity

import com.github.ykrasik.gamedex.datamodel.persistence.ExcludedPath
import com.github.ykrasik.gamedex.datamodel.persistence.Game
import com.github.ykrasik.gamedex.datamodel.persistence.Genre
import com.github.ykrasik.gamedex.datamodel.persistence.Library
import org.jetbrains.exposed.sql.ResultRow
import java.nio.file.Paths

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 11:21
 */

class OneToManyContext<T>(private val mapping: Map<Int, List<T>>) {
    operator fun get(id: Int): List<T> = requireNotNull(mapping[id]) { "No elements for id=$id!" }
}

object LibrariesMapper {
    fun map(row: ResultRow) = Library(
        id = row[Libraries.id].value,
        path = Paths.get(row[Libraries.path]),
        platform = row[Libraries.platform],
        name = row[Libraries.name]
    )
}
inline fun ResultRow.toLibrary(): Library = LibrariesMapper.map(this)

object GamesMapper {
    fun map(row: ResultRow, genres: OneToManyContext<Genre>, library: Library): Game {
        val id = row[Games.id].value
        return Game(
            id = id,
            path = Paths.get(row[Games.path]),
            name = row[Games.name],
            description = row[Games.description],
            releaseDate = row[Games.releaseDate]?.toLocalDate(),
            criticScore = row[Games.criticScore]?.toDouble(),
            userScore = row[Games.userScore]?.toDouble(),
            lastModified = row[Games.lastModified],
            metacriticUrl = row[Games.metacriticUrl],
            giantBombUrl = row[Games.giantBombUrl],
            genres = genres[id],
            library = library
        )
    }
}
inline fun ResultRow.toGame(genres: OneToManyContext<Genre>, library: Library): Game = GamesMapper.map(this, genres, library)

object GenresMapper {
    fun map(row: ResultRow) = Genre(
        id = row[Genres.id].value,
        name = row[Genres.name]
    )
}
inline fun ResultRow.toGenre(): Genre = GenresMapper.map(this)

object ExcludedPathsMapper {
    fun map(row: ResultRow) = ExcludedPath(
        id = row[ExcludedPaths.id].value,
        path = Paths.get(row[ExcludedPaths.path])
    )
}
inline fun ResultRow.toExcludedPath(): ExcludedPath = ExcludedPathsMapper.map(this)
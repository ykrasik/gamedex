package com.gitlab.ykrasik.gamedex.persistence.entity

import com.github.ykrasik.gamedex.common.Id
import com.github.ykrasik.gamedex.common.toId
import com.github.ykrasik.gamedex.common.toPath
import com.github.ykrasik.gamedex.datamodel.persistence.Game
import com.github.ykrasik.gamedex.datamodel.persistence.Genre
import com.github.ykrasik.gamedex.datamodel.persistence.Library
import org.jetbrains.exposed.sql.ResultRow

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 11:21
 */
object GamesMapper {
    fun map(row: ResultRow, genres: Map<Id<Game>, List<Genre>>, library: Library): Game {
        val id = row[Games.id].toId<Game>()
        return map(row, genres[id] ?: emptyList(), library)
    }

    fun map(row: ResultRow, genres: List<Genre>, library: Library): Game = Game(
        id = row[Games.id].toId(),
        path = row[Games.path].toPath(),
        name = row[Games.name],
        description = row[Games.description],
        releaseDate = row[Games.releaseDate]?.toLocalDate(),
        criticScore = row[Games.criticScore]?.toDouble(),
        userScore = row[Games.userScore]?.toDouble(),
        lastModified = row[Games.lastModified],
        metacriticUrl = row[Games.metacriticUrl],
        giantBombUrl = row[Games.giantBombUrl],
        genres = genres,
        library = library
    )
}
inline fun ResultRow.toGame(genres: Map<Id<Game>, List<Genre>>, library: Library): Game = GamesMapper.map(this, genres, library)
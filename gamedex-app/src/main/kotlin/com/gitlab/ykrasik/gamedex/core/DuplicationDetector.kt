package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.ProviderId
import org.joda.time.DateTime
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 16:48
 */
@Singleton
class DuplicationDetector {
    fun detectDuplications(games: List<Game>): GameDuplications {
        val headerToGames = games.asSequence()
            .flatMap { game -> game.providerHeaders.asSequence().map { it.withoutUpdateDate() to game } }
            .groupBy({ it.first }, { it.second })

        // Only detect duplications in the same platform.
        val duplicateHeaders = headerToGames
            .mapValues { (_, games) -> games.groupBy { it.platform }.filterValues { it.size > 1 }.flatMap { it.value } }
            .filterValues { it.size > 1 }

        val duplicateGames = duplicateHeaders.asSequence().flatMap { (header, games) ->
            games.asSequence().flatMap { game ->
                (games - game).asSequence().map { duplicatedGame ->
                    game to GameDuplication(header.id, duplicatedGame)
                }
            }
        }.groupBy({ it.first }, { it.second })

        return duplicateGames
    }

    private fun ProviderHeader.withoutUpdateDate() = copy(updateDate = DateTime(0))
}

typealias GameDuplications = Map<Game, List<GameDuplication>>
data class GameDuplication(
    val providerId: ProviderId,
    val duplicatedGame: Game
)
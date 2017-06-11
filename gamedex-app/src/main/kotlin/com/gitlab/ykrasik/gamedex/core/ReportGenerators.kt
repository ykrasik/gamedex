package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.ProviderId
import org.joda.time.DateTime
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 11:29
 */
typealias Report<T> = Map<Game, List<T>>

@Singleton
class GameDuplicationReportGenerator {
    fun detectDuplications(games: List<Game>): Report<GameDuplication> {
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

data class GameDuplication(
    val providerId: ProviderId,
    val duplicatedGame: Game
)

@Singleton
class NameFolderMismatchReportGenerator {
    fun detectGamesWithNameFolderMismatch(games: List<Game>): Report<GameNameFolderMismatch> {
        return games.asSequence()
            .flatMap { game -> game.rawGame.providerData.map { game to GameNameFolderMismatch(it.header.id, it.gameData.name) }.asSequence()}
            .filter { (game, mismatch) -> game.path.name != mismatch.expectedName }
            .groupBy({ it.first }, { it.second })
    }
}

data class GameNameFolderMismatch(
    val providerId: ProviderId,
    val expectedName: String
)
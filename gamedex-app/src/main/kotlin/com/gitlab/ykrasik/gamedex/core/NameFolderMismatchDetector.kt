package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ProviderId
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 10:03
 */
@Singleton
class NameFolderMismatchDetector {
    fun detectGamesWithNameFolderMismatch(games: List<Game>): GameNameFolderMismatches {
        return games.asSequence()
            .flatMap { game -> game.rawGame.providerData.map { game to GameNameFolderMismatch(it.header.id, it.gameData.name) }.asSequence()}
            .filter { (game, mismatch) -> game.path.name != mismatch.expectedName }
            .groupBy({ it.first }, { it.second })
    }
}

typealias GameNameFolderMismatches = Map<Game, List<GameNameFolderMismatch>>
data class GameNameFolderMismatch(
    val providerId: ProviderId,
    val expectedName: String
)
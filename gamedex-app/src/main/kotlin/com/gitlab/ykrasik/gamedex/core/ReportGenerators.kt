package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.ProviderId
import difflib.DiffUtils
import difflib.Patch
import org.joda.time.DateTime
import javax.inject.Inject
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
class NameFolderDiffReportGenerator @Inject constructor(private val sanitizer: NameSanitizer) {
    fun detectGamesWithNameFolderDiff(games: List<Game>): Report<GameNameFolderDiff> =
        games.asSequence().flatMap { game ->
            game.rawGame.providerData.mapNotNull { providerData ->
                val difference = diff(game, providerData) ?: return@mapNotNull null
                game to difference
            }.asSequence()
        }.groupBy({ it.first }, { it.second })

    private fun diff(game: Game, providerData: ProviderData): GameNameFolderDiff? {
        val actualName = sanitizer.removeMetaSymbols(game.path.name)
        val expectedName = sanitizer.toValidFileName(providerData.gameData.name)
        if (actualName == expectedName) return null

        val patch = DiffUtils.diff(actualName.toList(), expectedName.toList())
        return GameNameFolderDiff(
            providerId = providerData.header.id,
            actual = actualName,
            expected = expectedName,
            patch = patch
        )
    }
}

data class GameNameFolderDiff(
    val providerId: ProviderId,
    val actual: String,
    val expected: String,
    val patch: Patch<Char>
)
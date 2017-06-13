package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
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

// TODO: Add ignore case option
// TODO: Add option that makes metadata an optional match.
// TODO: Renaming loses the original metadata tags, fix it.
@Singleton
class NameFolderDiffReportGenerator @Inject constructor() {
    fun detectGamesWithNameFolderDiff(games: List<Game>): Report<GameNameFolderDiff> =
        games.asSequence().flatMap { game ->
            game.rawGame.providerData.mapNotNull { providerData ->
                val difference = diff(game, providerData) ?: return@mapNotNull null
                game to difference
            }.asSequence()
        }.groupBy({ it.first }, { it.second })

    private fun diff(game: Game, providerData: ProviderData): GameNameFolderDiff? {
        val folderMetaData = NameHandler.analyze(game.path.name)
        val actualName = folderMetaData.gameName
        val expectedName = NameHandler.toFileName(providerData.gameData.name)

        // TODO: This comparison needs to be smarter - account for metaTag.
        if (actualName == expectedName) return null

        val patch = DiffUtils.diff(actualName.toList(), expectedName.toList())
        return GameNameFolderDiff(
            providerId = providerData.header.id,
            actualName = folderMetaData,
            expected = expectedName,
            patch = patch
        )
    }
}

data class GameNameFolderDiff(
    val providerId: ProviderId,
    val actualName: FolderMetaData,
    val expected: String,
    val patch: Patch<Char>
)
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
        games.flatMap { game ->
            // TODO: If the majority of providers agree with the name, it is not a diff.
            game.rawGame.providerData.mapNotNull { providerData ->
                val difference = diff(game, providerData) ?: return@mapNotNull null
                game to difference
            }
        }.groupBy({ it.first }, { it.second })

    private fun diff(game: Game, providerData: ProviderData): GameNameFolderDiff? {
        val actual = game.folderMetaData
        val expected = expectedFrom(actual, providerData)
        if (actual == expected) return null

        val patch = DiffUtils.diff(actual.rawName.toList(), expected.rawName.toList())
        return GameNameFolderDiff(
            providerId = providerData.header.id,
            actual = actual,
            expected = expected,
            patch = patch
        )
    }

    private fun expectedFrom(actual: FolderMetaData, providerData: ProviderData): FolderMetaData {
        val gameName = NameHandler.toFileName(providerData.gameData.name)
        val expected = StringBuilder(gameName)
        // TODO: Add MetaTag
        actual.version?.let { version -> expected.append(" [$version]") }

        return FolderMetaData(
            rawName = expected.toString(),
            gameName = gameName,
            metaTag = null,
            version = actual.version
        )
    }
}

data class GameNameFolderDiff(
    val providerId: ProviderId,
    val actual: FolderMetaData,
    val expected: FolderMetaData,
    val patch: Patch<Char>
)
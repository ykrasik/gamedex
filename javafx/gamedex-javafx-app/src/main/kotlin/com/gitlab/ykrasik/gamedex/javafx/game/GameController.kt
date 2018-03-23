/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.javafx.game

import com.gitlab.ykrasik.gamdex.core.api.file.FileSystemService
import com.gitlab.ykrasik.gamdex.core.api.game.GamePresenter
import com.gitlab.ykrasik.gamdex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.game.Filter
import com.gitlab.ykrasik.gamedex.core.game.GameSettings
import com.gitlab.ykrasik.gamedex.core.matchesSearchQuery
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.javafx.notification.Notifier
import com.gitlab.ykrasik.gamedex.ui.view.game.edit.EditGameDataFragment
import com.gitlab.ykrasik.gamedex.ui.view.game.rename.RenameMoveFolderFragment
import com.gitlab.ykrasik.gamedex.ui.view.game.tag.TagFragment
import com.gitlab.ykrasik.gamedex.ui.view.main.MainView
import com.gitlab.ykrasik.gamedex.util.deleteWithChildren
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.withContext
import tornadofx.Controller
import java.nio.file.Files
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 14:39
 */
@Singleton
class GameController @Inject constructor(
    private val gameService: GameService,
    private val gamePresenter: GamePresenter,
    private val gameSettings: GameSettings,
    private val fileSystemService: FileSystemService,
    notifier: Notifier
) : Controller() {
    private val mainView: MainView by inject()

    private val logger = logger()

    val searchQueryProperty = SimpleStringProperty("")

    private val compositeFilterPredicate = run {
        val context = Filter.Context(emptyList(), fileSystemService)
        val filterPredicate = gameSettings.currentPlatformFilterSubject.toBindingCached().toPredicate { filter, game: Game ->
            filter!!.evaluate(game, context)
        }

        val searchPredicate = searchQueryProperty.toPredicate { query, game: Game -> game.matchesSearchQuery(query!!) }

        filterPredicate and searchPredicate
    }

    private val nameComparator = Comparator<Game> { o1, o2 -> o1.name.compareTo(o2.name, ignoreCase = true) }
    private val criticScoreComparator = compareBy(Game::criticScore)
    private val userScoreComparator = compareBy(Game::userScore)

    private val sortComparator = gameSettings.sortSubject.map { sort ->
        val comparator = when (sort.sortBy) {
            GameSettings.SortBy.name_ -> nameComparator
            GameSettings.SortBy.criticScore -> criticScoreComparator.then(nameComparator)
            GameSettings.SortBy.userScore -> userScoreComparator.then(nameComparator)
            GameSettings.SortBy.minScore -> compareBy<Game> { it.minScore }.then(criticScoreComparator).then(userScoreComparator).then(nameComparator)
            GameSettings.SortBy.avgScore -> compareBy<Game> { it.avgScore }.then(criticScoreComparator).then(userScoreComparator).then(nameComparator)
            GameSettings.SortBy.size -> compareBy<Game> { fileSystemService.sizeSync(it.path) }.then(nameComparator)        // FIXME: Hangs UI thread!!!
            GameSettings.SortBy.releaseDate -> compareBy(Game::releaseDate).then(nameComparator)
            GameSettings.SortBy.updateDate -> compareBy(Game::updateDate)
        }
        if (sort.order == GameSettings.SortType.asc) {
            comparator
        } else {
            comparator.reversed()
        }
    }.toBindingCached()

    val games: ObservableList<Game> = gameService.games.toObservableList()
    val platformGames = games.sortedFiltered().apply {
        filteredItems.predicateProperty().bind(gameSettings.platformSubject.toBindingCached().toPredicate { platform, game: Game ->
            game.platform == platform
        })
    }
    val sortedFilteredGames: ObservableList<Game> = platformGames.sortedFiltered().apply {
        filteredItems.predicateProperty().bind(compositeFilterPredicate)
        sortedItems.comparatorProperty().bind(sortComparator)
    }

    val genres = games.flatMapping(Game::genres).distincting().sorted()
    val tags = games.flatMapping(Game::tags).distincting()

    val canRunLongTask = notifier.canShowPersistentNotificationProperty     // FIXME: This is only here to tell us when we can show notifications, move this logic to a TaskManager.

    fun clearFilters() {
        gameSettings.currentPlatformFilter = Filter.`true`
        searchQueryProperty.value = ""
    }

    fun viewDetails(game: Game) = mainView.showGameDetails(game)
    suspend fun editDetails(game: Game, initialTab: GameDataType = GameDataType.name_): Game {
        val choice = EditGameDataFragment(game, initialTab).show()
        val overrides = when (choice) {
            is EditGameDataFragment.Choice.Override -> choice.overrides
            is EditGameDataFragment.Choice.Clear -> emptyMap()
            is EditGameDataFragment.Choice.Cancel -> return game
        }

        val newRawGame = game.rawGame.withDataOverrides(overrides)
        return if (newRawGame.userData != game.rawGame.userData) {
            gameService.replace(game, newRawGame)
        } else {
            game
        }
    }

    private fun RawGame.withDataOverrides(overrides: Map<GameDataType, GameDataOverride>): RawGame {
        // If new overrides are empty and userData is null, or userData has empty overrides -> nothing to do
        // If new overrides are not empty and userData is not null, but has the same overrides -> nothing to do
        if (overrides == userData?.overrides ?: emptyMap<GameDataType, GameDataOverride>()) return this

        val userData = this.userData ?: UserData()
        return copy(userData = userData.copy(overrides = overrides))
    }

    suspend fun tag(game: Game): Game {
        val choice = TagFragment(game).show()
        val tags = when (choice) {
            is TagFragment.Choice.Select -> choice.tags
            is TagFragment.Choice.Cancel -> return game
        }

        val newRawGame = game.rawGame.withTags(tags)
        return if (newRawGame.userData != game.rawGame.userData) {
            gameService.replace(game, newRawGame)
        } else {
            game
        }
    }

    private fun RawGame.withTags(tags: List<String>): RawGame {
        // If new tags are empty and userData is null, or userData has empty tags -> nothing to do
        // If new tags are not empty and userData is not null, but has the same tags -> nothing to do
        if (tags == userData?.tags ?: emptyList<String>()) return this

        val userData = this.userData ?: UserData()
        return copy(userData = userData.copy(tags = tags))
    }

    suspend fun scanNewGames() = gamePresenter.discoverNewGames()

    suspend fun rediscoverAllGamesWithoutAllProviders() = gamePresenter.rediscoverAllGamesWithMissingProviders()

    suspend fun rediscoverFilteredGamesWithoutAllProviders() = gamePresenter.rediscoverGames(sortedFilteredGames)

    suspend fun searchGame(game: Game) = gamePresenter.rediscoverGame(game)

    suspend fun refreshAllGames() = gamePresenter.redownloadAllGames()

    // TODO: This should appear in reports - refresh all games in report.
    suspend fun refreshFilteredGames() = gamePresenter.redownloadGames(sortedFilteredGames)

    suspend fun refreshGame(game: Game) = gamePresenter.redownloadGame(game)

    suspend fun renameFolder(game: Game, initialSuggestion: String? = null) = withContext(JavaFx) {
        val (library, newPath) = RenameMoveFolderFragment(game, initialSuggestion ?: game.path.name).show()
            ?: return@withContext

        withContext(CommonPool) {
            val fullPath = library.path.resolve(newPath)
            logger.info("Renaming/Moving: ${game.path} -> $fullPath")

            val parent = fullPath.parentFile
            if (parent != library.path && !parent.exists()) {
                parent.mkdirs()
            }
            if (!game.path.renameTo(fullPath)) {
                // File.renameTo is case sensitive, but can fail (doesn't cover all move variants).
                // If it does, retry with Files.move, which is platform-independent (but also case insensitive)
                // and throws an exception if it fails.
                Files.move(game.path.toPath(), fullPath.toPath())
            }

            gameService.replace(game, game.rawGame.withMetadata { it.copy(libraryId = library.id, path = newPath) })
        }
    }

    suspend fun delete(game: Game): Boolean = withContext(JavaFx) {
        val fromFileSystem = SimpleBooleanProperty(false)
        val confirm = areYouSureDialog("Delete game '${game.name}'?") {
            jfxToggleButton {
                text = "From File System"
                selectedProperty().bindBidirectional(fromFileSystem)
            }
        }
        if (!confirm) {
            return@withContext false
        }

        withContext(CommonPool) {
            if (fromFileSystem.value) {
                game.path.deleteWithChildren()
            }

            gameService.delete(game).run()
            true
        }
    }

    fun byId(id: Int): Game = gameService[id]
}
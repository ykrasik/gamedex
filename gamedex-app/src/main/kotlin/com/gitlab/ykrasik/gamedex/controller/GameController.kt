package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.task.GameTasks
import com.gitlab.ykrasik.gamedex.task.RefreshTasks
import com.gitlab.ykrasik.gamedex.task.SearchTasks
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.view.game.edit.EditGameDataFragment
import com.gitlab.ykrasik.gamedex.ui.view.game.tag.TagFragment
import com.gitlab.ykrasik.gamedex.ui.view.main.MainView
import javafx.beans.binding.BooleanBinding
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.control.TableColumn
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import tornadofx.Controller
import tornadofx.onChange
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 14:39
 */
@Singleton
class GameController @Inject constructor(
    private val gameRepository: GameRepository,
    private val gameTasks: GameTasks,
    private val searchTasks: SearchTasks,
    private val refreshTasks: RefreshTasks,
    private val settings: GameSettings
) : Controller() {
    private val mainView: MainView by inject()

    private val filtersForPlatformProperty = settings.filtersProperty.gettingOrElse(settings.platformProperty, GameSettings.FilterSet())
    val filteredLibrariesProperty = filtersForPlatformProperty.map { it!!.libraries }.apply { onChange { filterLibraries(it!!) } }
    val filteredGenresProperty = filtersForPlatformProperty.map { it!!.genres }.apply { onChange { filterGenres(it!!) } }
    val filteredTagsProperty = filtersForPlatformProperty.map { it!!.tags }.apply { onChange { filterTags(it!!) } }

    val searchQueryProperty = SimpleStringProperty("")

    private val compositeFilterPredicate = run {
        val libraryPredicate = filteredLibrariesProperty.toPredicate { libraries, game: Game ->
            listFilter(libraries) { game.library.id == it }
        }

        val genrePredicate = filteredGenresProperty.toPredicate { genres, game: Game ->
            listFilter(genres) { g -> game.genres.any { it == g } }
        }

        val tagPredicate = filteredTagsProperty.toPredicate { tags, game: Game ->
            listFilter(tags) { t -> game.tags.any { it == t } }
        }

        val searchPredicate = searchQueryProperty.toPredicate { query, game: Game ->
            query!!.isEmpty() || query.split(" ").all { word -> game.name.contains(word, ignoreCase = true) }
        }

        libraryPredicate and genrePredicate and tagPredicate and searchPredicate
    }

    private inline fun <T> listFilter(filteredItems: List<T>?, predicate: (T) -> Boolean) =
        filteredItems!!.isEmpty() || filteredItems.any(predicate)

    private val nameComparator = Comparator<Game> { o1, o2 -> o1.name.compareTo(o2.name, ignoreCase = true) }
    private val criticScoreComparator = compareBy(Game::criticScore)
    private val userScoreComparator = compareBy(Game::userScore)

    private val sortComparator = settings.sortProperty.map { sort ->
        val comparator = when (sort!!.sortBy) {
            GameSettings.SortBy.name_ -> nameComparator
            GameSettings.SortBy.criticScore -> criticScoreComparator.then(nameComparator)
            GameSettings.SortBy.userScore -> userScoreComparator.then(nameComparator)
            GameSettings.SortBy.minScore -> compareBy<Game> { it.minScore }.then(criticScoreComparator).then(userScoreComparator).then(nameComparator)
            GameSettings.SortBy.avgScore -> compareBy<Game> { it.avgScore }.then(criticScoreComparator).then(userScoreComparator).then(nameComparator)
            GameSettings.SortBy.releaseDate -> compareBy(Game::releaseDate).then(nameComparator)
            GameSettings.SortBy.updateDate -> compareBy(Game::updateDate)
        }
        if (sort.order == TableColumn.SortType.ASCENDING) {
            comparator
        } else {
            comparator.reversed()

        }
    }

    private val Game.minScore get() = criticScore?.let { c -> userScore?.let { u -> minOf(c.score, u.score) } }
    private val Game.avgScore get() = criticScore?.let { c -> userScore?.let { u -> (c.score + u.score) / 2 } }

    val allGames: ObservableList<Game> = gameRepository.games
    val platformGames = allGames.filtering(settings.platformProperty.toPredicateF { platform, game: Game ->
        game.platform == platform
    })
    val sortedFilteredGames: ObservableList<Game> = platformGames.sortedFiltered().apply {
        filteredItems.predicateProperty().bind(compositeFilterPredicate)
        sortedItems.comparatorProperty().bind(sortComparator)
    }

    val tags = allGames.flatMapping(Game::tags).distincting()

    val canRunLongTask: BooleanBinding get() = MainView.canShowPersistentNotificationProperty

    fun filterLibraries(libraries: List<Int>) = setFilters { it.copy(libraries = libraries) }
    fun filterGenres(genres: List<String>) = setFilters { it.copy(genres = genres) }
    fun filterTags(tags: List<String>) = setFilters { it.copy(tags = tags) }
    private fun setFilters(f: (GameSettings.FilterSet) -> GameSettings.FilterSet) {
        settings.filters += (settings.platform to f(filtersForPlatformProperty.value))
    }

    fun clearFilters() {
        settings.filters += (settings.platform to GameSettings.FilterSet())
        searchQueryProperty.value = ""
    }

    fun viewDetails(game: Game) = mainView.showGameDetails(game)
    fun editDetails(game: Game, initialTab: GameDataType = GameDataType.name_): Deferred<Game> = async(JavaFx) {
        val choice = EditGameDataFragment(game, initialTab).show()
        val overrides = when (choice) {
            is EditGameDataFragment.Choice.Override -> choice.overrides
            is EditGameDataFragment.Choice.Clear -> emptyMap()
            is EditGameDataFragment.Choice.Cancel -> return@async game
        }

        val newRawGame = game.rawGame.withDataOverrides(overrides)
        if (newRawGame.userData != game.rawGame.userData) {
            gameRepository.update(newRawGame)
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

    fun tag(game: Game): Deferred<Game> = async(JavaFx) {
        val choice = TagFragment(game).show()
        val tags = when (choice) {
            is TagFragment.Choice.Select -> choice.tags
            is TagFragment.Choice.Cancel -> return@async game
        }

        val newRawGame = game.rawGame.withTags(tags)
        if (newRawGame.userData != game.rawGame.userData) {
            gameRepository.update(newRawGame)
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

    fun scanNewGames() = gameTasks.ScanNewGamesTask().apply { start() }
    fun cleanup(): GameTasks.CleanupTask {
        // TODO: Detect stale games, confirm, then delete.
        return gameTasks.CleanupTask().apply { start() }
    }

    fun rediscoverGamesWithoutProviders() = searchTasks.RediscoverAllGamesTask().apply { start() }
    fun rediscoverFilteredGames() = searchTasks.RediscoverGamesTask(sortedFilteredGames).apply { start() }
    fun searchGame(game: Game) = searchTasks.SearchGameTask(game).apply { start() }

    fun refreshAllGames() = refreshTasks.RefreshGamesTask(gameRepository.games).apply { start() }
    fun refreshFilteredGames() = refreshTasks.RefreshGamesTask(sortedFilteredGames).apply { start() }
    fun refreshGame(game: Game) = refreshTasks.RefreshGameTask(game).apply { start() }

    fun delete(game: Game): Boolean {
        if (!areYouSureDialog("Delete game '${game.name}'?")) return false

        launch(JavaFx) {
            gameRepository.delete(game)
            MainView.showFlashInfoNotification("Deleted game: '${game.name}'.")
        }
        return true
    }
}
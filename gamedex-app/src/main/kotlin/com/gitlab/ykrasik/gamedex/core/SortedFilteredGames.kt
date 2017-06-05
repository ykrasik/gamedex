package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.*
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.control.TableColumn
import tornadofx.SortedFilteredList
import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 06/05/2017
 * Time: 12:48
 */
class SortedFilteredGames(_games: ObservableList<Game>) {
    val platformProperty = SimpleObjectProperty<Platform>()
    var platform by platformProperty

    val filtersProperty = SimpleObjectProperty(emptyMap<Platform, GameSettings.FilterSet>())
    var filters by filtersProperty

    val filtersForPlatformProperty = filtersProperty.gettingOrElse(platformProperty, GameSettings.FilterSet())

    val searchQueryProperty = SimpleStringProperty("")
    var searchQuery by searchQueryProperty

    val sortProperty = SimpleObjectProperty(GameSettings.Sort())
    var sort by sortProperty

    val games: ObservableList<Game> = _games.sortedFiltered()

    private val nameComparator = Comparator<Game> { o1, o2 -> o1.name.compareTo(o2.name, ignoreCase = true) }
    private val criticScoreComparator = compareBy(Game::criticScore)
    private val userScoreComparator = compareBy(Game::userScore)

    init {
        val platformPredicate = platformProperty.toPredicate { platform, game: Game ->
            game.platform == platform
        }

        val libraryPredicate = filtersForPlatformProperty.toPredicate { filters, game: Game ->
            filters!!.libraries.let { libraries ->
                libraries.isEmpty() || libraries.any { game.library.id == it }
            }
        }

        val genrePredicate = filtersForPlatformProperty.toPredicate { filters, game: Game ->
            filters!!.genres.let { genres ->
                genres.isEmpty() || genres.any { g -> game.genres.any { it == g } }
            }
        }

        val tagPredicate = filtersForPlatformProperty.toPredicate { filters, game: Game ->
            filters!!.tags.let { tags ->
                tags.isEmpty() || tags.any { t -> game.tags.any { it == t } }
            }
        }

        val searchPredicate = searchQueryProperty.toPredicate { query, game: Game ->
            query!!.isEmpty() || query.split(" ").all { word -> game.name.contains(word, ignoreCase = true) }
        }

        val gameFilterPredicate =
            platformPredicate and libraryPredicate and genrePredicate and tagPredicate and searchPredicate

        games as SortedFilteredList<Game>
        games.filteredItems.predicateProperty().bind(gameFilterPredicate)
        games.sortedItems.comparatorProperty().bind(sortComparator())
    }

    fun filterLibraries(libraries: List<Library>) = setFilters { it.copy(libraries = libraries.map { it.id }) }
    fun filterGenres(genres: List<String>) = setFilters { it.copy(genres = genres) }
    fun filterTags(tags: List<String>) = setFilters { it.copy(tags = tags) }

    fun clearFilters() {
        filters += (platform to GameSettings.FilterSet())
        searchQuery = ""
    }

    private fun setFilters(f: (GameSettings.FilterSet) -> GameSettings.FilterSet) {
        filters += (platform to f(filtersForPlatformProperty.value))
    }

    private fun sortComparator() = sortProperty.map { sort ->
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
}
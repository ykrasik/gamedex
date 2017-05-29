package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.and
import com.gitlab.ykrasik.gamedex.ui.gettingOrElse
import com.gitlab.ykrasik.gamedex.ui.sortedFiltered
import com.gitlab.ykrasik.gamedex.ui.toPredicate
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.control.TableColumn
import tornadofx.SortedFilteredList
import tornadofx.getValue
import tornadofx.onChange
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 06/05/2017
 * Time: 12:48
 */
class SortedFilteredGames(_games: ObservableList<Game>) {
    val platformFilterProperty = SimpleObjectProperty<Platform>()
    var platformFilter by platformFilterProperty

    val sourceIdsPerPlatformFilterProperty = SimpleObjectProperty(emptyMap<Platform, List<Int>>())
    var sourceIdsPerPlatformFilter by sourceIdsPerPlatformFilterProperty

    val sourceIdsFilterProperty = sourceIdsPerPlatformFilterProperty.gettingOrElse(platformFilterProperty, emptyList())
    val sourceIdsFilter by sourceIdsFilterProperty

    val genreFilterProperty = SimpleStringProperty(allGenres)
    var genreFilter by genreFilterProperty

    val tagFilterProperty = SimpleStringProperty(allTags)
    var tagFilter by tagFilterProperty

    val searchQueryProperty = SimpleStringProperty("")
    var searchQuery by searchQueryProperty

    val sortProperty = SimpleObjectProperty<GameSettings.Sort>(GameSettings.Sort.name_)
    var sort by sortProperty

    val sortOrderProperty = SimpleObjectProperty<TableColumn.SortType>(TableColumn.SortType.ASCENDING)
    var sortOrder by sortOrderProperty

    val games: ObservableList<Game> = _games.sortedFiltered()

    private val nameComparator = Comparator<Game> { o1, o2 -> o1.name.compareTo(o2.name, ignoreCase = true) }
    private val criticScoreComparator = compareBy(Game::criticScore)
    private val userScoreComparator = compareBy(Game::userScore)

    init {
        val platformPredicate = platformFilterProperty.toPredicate { platform, game: Game ->
            game.platform == platform
        }

        val sourcePredicate = sourceIdsFilterProperty.toPredicate { sourceIds, game: Game ->
            sourceIds!!.isEmpty() || sourceIds.any { game.library.id == it }
        }

        val searchPredicate = searchQueryProperty.toPredicate { query, game: Game ->
            query!!.isEmpty() || query.split(" ").all { word -> game.name.contains(word, ignoreCase = true) }
        }

        val genrePredicate = genreFilterProperty.toPredicate { genre, game: Game ->
            genre.isNullOrEmpty() || genre == allGenres || game.genres.contains(genre)
        }

        val tagPredicate = tagFilterProperty.toPredicate { tag, game: Game ->
            tag.isNullOrEmpty() || tag == allTags || game.tags.contains(tag)
        }

        val gameFilterPredicate = platformPredicate and sourcePredicate and
            searchPredicate and genrePredicate and tagPredicate

        games as SortedFilteredList<Game>
        games.filteredItems.predicateProperty().bind(gameFilterPredicate)
        games.sortedItems.comparatorProperty().bind(sortComparator())
    }

    fun clearFilters() {
        genreFilter = allGenres
        tagFilter = allTags
        searchQuery = ""
    }

    private fun sortComparator(): ObjectProperty<Comparator<Game>> {
        fun comparator(): Comparator<Game> {
            val comparator = when (sortProperty.value!!) {
                GameSettings.Sort.name_ -> nameComparator
                GameSettings.Sort.criticScore -> criticScoreComparator.then(nameComparator)
                GameSettings.Sort.userScore -> userScoreComparator.then(nameComparator)
                GameSettings.Sort.minScore -> compareBy<Game> { it.minScore }.then(criticScoreComparator).then(userScoreComparator).then(nameComparator)
                GameSettings.Sort.avgScore -> compareBy<Game> { it.avgScore }.then(criticScoreComparator).then(userScoreComparator).then(nameComparator)
                GameSettings.Sort.releaseDate -> compareBy(Game::releaseDate).then(nameComparator)
                GameSettings.Sort.updateDate -> compareBy(Game::updateDate)
            }
            return if (sortOrderProperty.value == TableColumn.SortType.ASCENDING) {
                comparator
            } else {
                comparator.reversed()
            }
        }

        val property = SimpleObjectProperty(comparator())
        sortOrderProperty.onChange {
            property.value = comparator()
        }
        sortProperty.onChange {
            property.value = comparator()
        }
        return property
    }

    private val Game.minScore get() = criticScore?.let { c -> userScore?.let { u -> minOf(c.score, u.score) } }
    private val Game.avgScore get() = criticScore?.let { c -> userScore?.let { u -> (c.score + u.score) / 2 } }

    companion object {
        val allGenres = "All"
        val allTags = "All"
    }
}
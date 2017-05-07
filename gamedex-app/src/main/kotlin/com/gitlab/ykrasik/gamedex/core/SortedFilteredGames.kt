package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.preferences.GameSort
import com.gitlab.ykrasik.gamedex.ui.and
import com.gitlab.ykrasik.gamedex.ui.mapProperty
import com.gitlab.ykrasik.gamedex.ui.toPredicate
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import tornadofx.SortedFilteredList

/**
 * User: ykrasik
 * Date: 06/05/2017
 * Time: 12:48
 */
class SortedFilteredGames(
    val platformFilterProperty: ObjectProperty<Platform>,
    val sortProperty: ObjectProperty<GameSort>,
    _games: ObservableList<Game>
) {
    val searchQueryProperty = SimpleStringProperty("")
    val genreFilterProperty = SimpleStringProperty("")

    val games: ObservableList<Game> = SortedFilteredList(_games)

    private val nameComparator = compareBy(Game::name)
    private val criticScoreComparator = compareBy(Game::criticScore).then(nameComparator)
    private val userScoreComparator = compareBy(Game::userScore).then(nameComparator)
    private val releaseDateComparator = compareBy(Game::releaseDate).then(nameComparator)
    private val dateAddedComparator = compareBy(Game::lastModified)

    init {
        val platformPredicate = platformFilterProperty.toPredicate { platform, game: Game ->
            game.platform == platform
        }

        val searchPredicate = searchQueryProperty.toPredicate { query, game: Game ->
            query!!.isEmpty() || game.name.contains(query, ignoreCase = true)
        }

        val genrePredicate = genreFilterProperty.toPredicate { genre, game: Game ->
            genre.isNullOrEmpty() || game.genres.contains(genre)
        }

        val gameFilterPredicateProperty = platformPredicate.and(searchPredicate).and(genrePredicate)

        games as SortedFilteredList<Game>
        games.filteredItems.predicateProperty().bind(gameFilterPredicateProperty)
        games.sortedItems.comparatorProperty().bind(sortProperty.mapProperty { it!!.toComparator() })
    }

    private fun GameSort.toComparator(): Comparator<Game>? = when (this) {
        GameSort.nameAsc -> nameComparator
        GameSort.nameDesc -> nameComparator.reversed()
        GameSort.criticScoreAsc -> criticScoreComparator
        GameSort.criticScoreDesc -> criticScoreComparator.reversed()
        GameSort.userScoreAsc -> userScoreComparator
        GameSort.userScoreDesc -> userScoreComparator.reversed()
        GameSort.releaseDateAsc -> releaseDateComparator
        GameSort.releaseDateDesc -> releaseDateComparator.reversed()
        GameSort.dateAddedAsc -> dateAddedComparator
        GameSort.dateAddedDesc -> dateAddedComparator.reversed()
        else -> null
    }
}
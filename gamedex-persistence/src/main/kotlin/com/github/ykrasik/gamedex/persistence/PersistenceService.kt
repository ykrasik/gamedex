package com.github.ykrasik.gamedex.persistence

import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.ImageData
import com.github.ykrasik.gamedex.datamodel.persistence.ExcludedPath
import com.github.ykrasik.gamedex.datamodel.persistence.Game
import com.github.ykrasik.gamedex.datamodel.persistence.Genre
import com.github.ykrasik.gamedex.datamodel.persistence.Library
import com.github.ykrasik.gamedex.datamodel.provider.GameData
import java.nio.file.Path

/**
 * @author Yevgeny Krasik
 */
// TODO: Inconsistent responsibility between the service and the managers - sometimes managers return from cache,
// TODO: sometimes they query the service. Should be Either Or.
interface PersistenceService {
    interface Games {
        val all: List<Game>
        operator fun get(id: Int): Game
        operator fun contains(path: Path): Boolean
        fun add(gameData: GameData, path: Path, library: Library): Game
        fun delete(id: Int)

        fun getThumbnail(id: Int): ImageData?
        fun getPoster(id: Int): ImageData?
    }

    interface Genres {
        val all: List<Genre>
    }

    interface Libraries {
        val all: List<Library>
        operator fun get(id: Int): Library
        operator fun contains(path: Path): Boolean
        fun add(path: Path, platform: GamePlatform, name: String): Library
        fun delete(id: Int): List<Int>
    }

    interface ExcludedPaths {
        val all: List<ExcludedPath>
        operator fun contains(path: Path): Boolean
        fun add(path: Path): ExcludedPath
        fun delete(id: Int)
    }

    val games: Games
    val genres: Genres
    val libraries: Libraries
    val excludedPaths: ExcludedPaths
}

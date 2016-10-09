package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.TimeProvider
import com.github.ykrasik.gamedex.common.toId
import com.github.ykrasik.gamedex.common.toPath
import com.github.ykrasik.gamedex.datamodel.*
import com.github.ykrasik.gamedex.datamodel.provider.GameData
import com.gitlab.ykrasik.gamedex.persistence.TestDbInitializer
import com.nhaarman.mockito_kotlin.atLeast
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.kotlintest.specs.StringSpec
import org.joda.time.DateTime
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 06/10/2016
 * Time: 20:55
 */
abstract class DaoTest : StringSpec() {
    val now = DateTime.now()

    private val timeProvider = mock<TimeProvider> {
        on { now() } doReturn now
    }

    // TODO: This is probably not a scalable solution, but for this low amount of DAOs it works.
    // TODO: Refactor if amount of DAOs grows.
    val genreDao = GenreDaoImpl()
    val gameDao = GameDaoImpl(genreDao, timeProvider)
    val libraryDao = LibraryDaoImpl()
    val excludedPathDao = ExcludedPathDaoImpl()

    override fun beforeEach() {
        TestDbInitializer.reload()
    }

    fun givenLibraryExists(id: Int, rawPath: String, platform: GamePlatform = GamePlatform.PC, name: String = rawPath): Library {
        val path = rawPath.toPath()
        val library = libraryDao.add(path, platform, name)
        library shouldBe Library(id.toId(), path, platform, name)
        return library
    }

    fun givenGameExists(gameId: Int,
                        library: Library,
                        path: String = gameId.toString(),
                        genres: List<Genre> = emptyList(),
                        description: String? = null,
                        releaseDate: LocalDate? = null,
                        criticScore: Double? = null,
                        userScore: Double? = null,
                        giantBombUrl: String? = null): Game {
        val gameData = GameData(
            name = gameId.toString(),
            description = description,
            releaseDate = releaseDate,
            criticScore = criticScore,
            userScore = userScore,
            thumbnail = null,
            poster = null,
            genres = genres.map { it.name },
            metacriticUrl = "metacritic",
            giantBombUrl = giantBombUrl
        )
        val game = gameDao.add(gameData, path.toPath(), library)
        verify(timeProvider, atLeast(1)).now()

        game shouldBe Game(
            id = gameId.toId(),
            path = path.toPath(),
            name = gameData.name,
            description = gameData.description,
            releaseDate = gameData.releaseDate,
            criticScore = gameData.criticScore,
            userScore = gameData.userScore,
            lastModified = now,
            metacriticUrl = gameData.metacriticUrl,
            giantBombUrl = gameData.giantBombUrl,
            genres = genres,
            library = library
        )
        return game
    }

    fun givenGenreExists(id: Int, name: String): Genre {
        val genre = genreDao.add(name)
        genre shouldBe Genre(id.toId(), name)
        return genre
    }

    fun givenGenresAreLinkedToGame(game: Game, vararg genres: Genre) {
        genreDao.linkedGames.link(game.id, genres.toList())
    }

    fun givenExcludedPathExists(id: Int, rawPath: String): ExcludedPath {
        val path = rawPath.toPath()
        val excludedPath = excludedPathDao.add(path)
        excludedPath shouldBe ExcludedPath(id.toId(), path)
        return excludedPath
    }
}
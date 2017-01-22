package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.Library
import org.h2.jdbc.JdbcSQLException
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 15:41
 */
class GameDaoTest : DaoTest() {
    val dao = gameDao

    lateinit var library: Library

    override fun beforeEach() {
        super.beforeEach()
        library = givenLibraryExists(1, "library")
    }

    init {
        "Insert and retrieve a game" {
            val genre1 = Genre(1.toId(), "genre1")
            val genre2 = Genre(2.toId(), "genre2")

            val game = givenGameExists(
                gameId = 1,
                library = library,
                path = "path/to/game",
                genres = listOf(genre1, genre2),
                description = "desc",
                releaseDate = LocalDate.now(),
                criticScore = 1.2,
                userScore = 3.4,
                giantBombUrl = "giantBomb"
            )

            dao.get(1.toId()) shouldBe game
            dao.all shouldBe listOf(game)
        }

        "Insert and retrieve multiple games" {
            val library2 = givenLibraryExists(2, "library2")

            val genre1 = givenGenreExists(1, "genre1")
            val genre2 = Genre(2.toId(), "genre2")
            val genre3 = Genre(3.toId(), "genre3")

            val game1 = givenGameExists(
                gameId = 1,
                library = library,
                path = "game1",
                genres = listOf(genre1, genre2),
                description = "desc1",
                releaseDate = LocalDate.now(),
                criticScore = 1.2,
                userScore = 3.4,
                giantBombUrl = "giantBomb"
            )
            val game2 = givenGameExists(
                gameId = 2,
                library = library,
                path = "game2",
                genres = listOf(genre2, genre3),
                description = "desc2",
                criticScore = 5.6
            )
            val game3 = givenGameExists(
                gameId = 3,
                library = library2,
                path = "game3",
                genres = listOf(genre1, genre3),
                userScore = 3.4,
                giantBombUrl = "giantBomb"
            )

            dao.get(1.toId()) shouldBe game1
            dao.get(2.toId()) shouldBe game2
            dao.get(3.toId()) shouldBe game3
            dao.all shouldBe listOf(game1, game2, game3)
        }

        "Check that a game exists for a path" {
            val path = "path/to/game"
            givenGameExists(1, library, path)

            dao.exists(path.toPath()) shouldBe true
            dao.exists((path + "2").toPath()) shouldBe false
        }

        "Delete a game without genres" {
            val game = givenGameExists(1, library)

            dao.delete(game)

            dao.all shouldBe emptyList<Game>()
        }

        "Delete a game being the only one linked to a genre - the genre must also be deleted" {
            val genre = givenGenreExists(1, "genre1")
            val game = givenGameExists(1, library, genres = listOf(genre))

            dao.delete(game)

            dao.all shouldBe emptyList<Game>()
            genreDao.all shouldBe emptyList<Genre>()
        }

        "Delete a game not being the only one linked to a genre - the genre must not be deleted" {
            val genre = givenGenreExists(1, "genre")
            val game1 = givenGameExists(1, library, genres = listOf(genre))
            val game2 = givenGameExists(2, library, genres = listOf(genre))

            dao.delete(game1)

            dao.all shouldBe listOf(game2)
            genreDao.all shouldBe listOf(genre)
        }

        "Throw an exception when trying to retrieve a non-existing game" {
            givenGameExists(1, library)

            shouldThrow<IllegalArgumentException> {
                dao.get(2.toId())
            }
        }

        "Throw an exception when trying to insert a game for an already existing path" {
            val path = "path/to/game"
            givenGameExists(1, library, path)

            val library2 = givenLibraryExists(2, "library2")

            shouldThrow<JdbcSQLException> {
                // Too much typing... so we called this method again.
                givenGameExists(2, library2, path)
            }
        }

        "Throw an exception when trying to insert a game for a non-existing library" {
            val library = Library(2.toId(), "".toPath(), "", GamePlatform.pc)

            shouldThrow<JdbcSQLException> {
                // Too much typing... so we called this method again.
                givenGameExists(1, library)
            }
        }

        "Throw an exception when trying to delete a non-existing game" {
            givenGameExists(1, library)

            shouldThrow<IllegalArgumentException> {
                val invalidGame = Game(2.toId(), "".toPath(), "", null, null, null, null, now, "", null, emptyList(), library)
                dao.delete(invalidGame)
            }
        }
    }
}
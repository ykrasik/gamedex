package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.toId
import com.github.ykrasik.gamedex.datamodel.persistence.Game
import com.github.ykrasik.gamedex.datamodel.persistence.Genre
import com.github.ykrasik.gamedex.datamodel.persistence.Library
import org.h2.jdbc.JdbcSQLException

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 09:46
 */
class GameGenreDaoTest : DaoTest() {
    val dao = genreDao.linkedGames

    lateinit var library: Library
    lateinit var game1: Game
    lateinit var game2: Game
    val gameId1 = 1.toId<Game>()
    val gameId2 = 2.toId<Game>()
    val invalidGameId = 3

    override fun beforeEach() {
        super.beforeEach()
        library = givenLibraryExists(1, "library1")
        game1 = givenGameExists(gameId1.id, library)
        game2 = givenGameExists(gameId2.id, library)
    }

    init {
        "Link a single genre to a game and retrieve that link" {
            val genre = givenGenreExists(1, "genre")
            givenGenresAreLinkedToGame(game1, genre)

            val expectedGameGenres = listOf(genre)
            dao.getByGame(gameId1) shouldBe expectedGameGenres
            dao.all shouldBe mapOf(gameId1 to expectedGameGenres)
        }

        "Link multiple genres to multiple games and retrieve those links" {
            val genre1 = givenGenreExists(1, "genre1")
            val genre2 = givenGenreExists(2, "genre2")
            givenGenresAreLinkedToGame(game1, genre1, genre2)

            val genre3 = givenGenreExists(3, "genre3")
            givenGenresAreLinkedToGame(game2, genre3)

            val expectedGame1Genres = listOf(genre1, genre2)
            val expectedGame2Genres = listOf(genre3)
            dao.getByGame(gameId1) shouldBe expectedGame1Genres
            dao.getByGame(gameId2) shouldBe expectedGame2Genres
            dao.all shouldBe mapOf(gameId1 to expectedGame1Genres, gameId2 to expectedGame2Genres)
        }

        "Unlink all genres from a game" {
            val genre1 = givenGenreExists(1, "genre1")
            val genre2 = givenGenreExists(2, "genre2")
            givenGenresAreLinkedToGame(game1, genre1, genre2)

            dao.unlinkAll(game1) shouldBe 2

            dao.getByGame(gameId1) shouldBe emptyList<Genre>()
        }

        "Throw an exception if trying to link a genre to a game which is already linked" {
            val genre1 = givenGenreExists(1, "genre1")
            givenGenresAreLinkedToGame(game1, genre1)

            shouldThrow<JdbcSQLException> {
                dao.link(gameId1, listOf(genre1))
            }
        }

        "Throw an exception if trying to link to a non-existing game" {
            val genre1 = givenGenreExists(1, "genre1")

            shouldThrow<JdbcSQLException> {
                dao.link(invalidGameId.toId(), listOf(genre1))
            }
        }

        "Throw an exception if trying to link to a non-existing genre" {
            val genre1 = Genre(1.toId(), "genre1")

            shouldThrow<JdbcSQLException> {
                dao.link(gameId1, listOf(genre1))
            }
        }

        "Throw an exception if trying to delete a genre that is linked to a game" {
            val genre1 = givenGenreExists(1, "genre1")
            givenGenresAreLinkedToGame(game1, genre1)

            shouldThrow<JdbcSQLException> {
                genreDao.delete(listOf(genre1))
            }
        }

        "Throw an exception if trying to delete a game that is linked to a genre" {
            val genre1 = givenGenreExists(1, "genre1")
            givenGenresAreLinkedToGame(game1, genre1)

            shouldThrow<JdbcSQLException> {
                gameDao.delete(game1)
            }
        }
    }
}
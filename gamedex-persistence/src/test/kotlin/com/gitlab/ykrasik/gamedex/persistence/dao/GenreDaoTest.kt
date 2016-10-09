package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.toId
import com.github.ykrasik.gamedex.datamodel.Genre
import org.h2.jdbc.JdbcSQLException

/**
 * User: ykrasik
 * Date: 06/10/2016
 * Time: 21:07
 */
class GenreDaoTest : DaoTest() {
    val dao = genreDao

    init {
        "Insert and retrieve a genre" {
            val genre1 = givenGenreExists(1, "genre1")

            dao.get("genre1") shouldBe genre1
            dao.get("genre2") shouldBe null
        }

        "Retrieve all existing genres" {
            val genre1 = givenGenreExists(1, "genre1")
            val genre2 = givenGenreExists(2, "genre2")
            val genre3 = givenGenreExists(3, "genre3")

            dao.all shouldBe listOf(genre1, genre2, genre3)
        }

        "getOrAdd returns an existing genre when it exists" {
            // TODO: This doesn't really test this case, lacks an assertion that verifies
            val genre = givenGenreExists(1, "genre")
            dao.getOrAdd("genre") shouldBe genre
        }

        "getOrAdd inserts a new genre when it doesn't exist" {
            // TODO: This doesn't really test this case, lacks an assertion that verifies
            dao.getOrAdd("genre") shouldBe Genre(1.toId(), "genre")
        }

        "Delete a single genre" {
            val genre1 = givenGenreExists(1, "genre1")
            val genre2 = givenGenreExists(2, "genre2")
            val genre3 = givenGenreExists(3, "genre3")

            dao.delete(listOf(genre2)) shouldBe 1
            dao.all shouldBe listOf(genre1, genre3)
        }

        "Delete multiple genres" {
            val genre1 = givenGenreExists(1, "genre1")
            val genre2 = givenGenreExists(2, "genre2")
            val genre3 = givenGenreExists(3, "genre3")

            dao.delete(listOf(genre1, genre3)) shouldBe 2
            dao.all shouldBe listOf(genre2)
        }

        "Delete multiple genres but ignore genres that don't exist" {
            val genre1 = givenGenreExists(1, "genre1")
            val genre2 = givenGenreExists(2, "genre2")
            val genre3 = givenGenreExists(3, "genre3")
            val invalidGenre = Genre(4.toId(), "invalidGenre")

            dao.delete(listOf(genre1, genre3, invalidGenre)) shouldBe 2
            dao.all shouldBe listOf(genre2)
        }

        "Do not delete anything when no genres match the delete criteria" {
            val genre1 = givenGenreExists(1, "genre1")
            val invalidGenre1 = Genre(2.toId(), "invalidGenre1")
            val invalidGenre2 = Genre(3.toId(), "invalidGenre2")

            dao.delete(listOf(invalidGenre1, invalidGenre2)) shouldBe 0
            dao.all shouldBe listOf(genre1)
        }

        "Throw an exception when trying to insert the same genre twice" {
            givenGenreExists(1, "genre")

            shouldThrow<JdbcSQLException> {
                dao.add("genre")
            }
        }
    }
}
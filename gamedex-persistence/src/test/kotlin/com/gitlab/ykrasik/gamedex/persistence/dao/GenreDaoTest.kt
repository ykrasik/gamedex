package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.datamodel.persistence.Genre
import org.h2.jdbc.JdbcSQLException
import org.junit.Assert.assertEquals

/**
 * User: ykrasik
 * Date: 06/10/2016
 * Time: 21:07
 */
class GenreDaoTest : BaseDaoTest() {
    val dao = GenreDaoImpl()

    init {
        "GenreDao" should {
            "insert and retrieve genre" {
                val genre1 = givenGenreExists("genre1", 1)
                dao.getByName("genre1") shouldBe genre1
                dao.getByName("genre2") shouldBe null
            }

            "retrieve all existing genres" {
                val genre1 = givenGenreExists("genre1", 1)
                val genre2 = givenGenreExists("genre2", 2)
                val genre3 = givenGenreExists("genre3", 3)

                dao.all shouldBe listOf(genre1, genre2, genre3)
            }

            "getOrAdd returns existing genre when it exists" {
                val genre = givenGenreExists("genre", 1)
                dao.getOrAdd("genre") shouldBe genre
            }

            "getOrAdd inserts a new genre when it doesn't exist" {
                dao.getOrAdd("genre") shouldBe Genre(1, "genre")
            }

            "delete single genre by id" {
                val genre1 = givenGenreExists("genre1", 1)
                val genre2 = givenGenreExists("genre2", 2)
                val genre3 = givenGenreExists("genre3", 3)

                dao.deleteByIds(listOf(2)) shouldBe 1
                dao.all shouldBe listOf(genre1, genre3)
            }

            "delete multiple genres by id" {
                val genre1 = givenGenreExists("genre1", 1)
                val genre2 = givenGenreExists("genre2", 2)
                val genre3 = givenGenreExists("genre3", 3)

                dao.deleteByIds(listOf(1, 3)) shouldBe 2
                dao.all shouldBe listOf(genre2)
            }

            "delete multiple genres by id, but ignore ids that don't exist" {
                val genre1 = givenGenreExists("genre1", 1)
                val genre2 = givenGenreExists("genre2", 2)
                val genre3 = givenGenreExists("genre3", 3)

                dao.deleteByIds(listOf(1, 3, 4)) shouldBe 2
                dao.all shouldBe listOf(genre2)
            }

            "not delete anything when no genre ids match the delete criteria" {
                val genre1 = givenGenreExists("genre1", 1)
                val genre2 = givenGenreExists("genre2", 2)
                val genre3 = givenGenreExists("genre3", 3)

                dao.deleteByIds(listOf(4, 5, 6)) shouldBe 0
                dao.all shouldBe listOf(genre1, genre2, genre3)
            }

            "throw an exception when trying to insert the same genre twice" {
                givenGenreExists("genre", 1)
                shouldThrow<JdbcSQLException> {
                    dao.add("genre")
                }
            }
        }
    }

    private fun givenGenreExists(name: String, expectedId: Int): Genre = dao.add(name).apply {
        assertEquals(Genre(expectedId, name), this)
    }
}
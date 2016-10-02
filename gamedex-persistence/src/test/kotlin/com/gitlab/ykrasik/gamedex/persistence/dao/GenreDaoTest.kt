package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.datamodel.persistence.Genre
import org.h2.jdbc.JdbcSQLException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * User: ykrasik
 * Date: 06/10/2016
 * Time: 21:07
 */
class GenreDaoTest : AbstractDaoTest() {
    val dao = GenreDaoImpl()

    @Test
    fun insertAndRetrieve() {
        val genre1 = givenGenreExists("genre1", 1)
        assertEquals(genre1, dao.getByName("genre1"))
        assertNull(dao.getByName("genre2"))
    }

    @Test
    fun returnAllExistingEntries() {
        val genre1 = givenGenreExists("genre1", 1)
        val genre2 = givenGenreExists("genre2", 2)
        val genre3 = givenGenreExists("genre3", 3)

        assertEquals(listOf(genre1, genre2, genre3), dao.all)
    }

    @Test
    fun doesntGetOrAddExisting() {
        val genre = givenGenreExists("genre", 1)
        assertEquals(genre, dao.getOrAdd("genre"))
    }

    @Test
    fun getOrAddNew() {
        val genre = Genre(1, "genre")
        assertEquals(genre, dao.getOrAdd("genre"))
    }

    @Test
    fun deleteMultipleEntries() {
        val genre1 = givenGenreExists("genre1", 1)
        val genre2 = givenGenreExists("genre2", 2)
        val genre3 = givenGenreExists("genre3", 3)

        assertEquals(2, dao.deleteByIds(listOf(1, 3)))
        assertEquals(listOf(genre2), dao.all)
    }

    @Test
    fun deleteMultipleEntriesAndIgnoresNotExisting() {
        val genre1 = givenGenreExists("genre1", 1)
        val genre2 = givenGenreExists("genre2", 2)
        val genre3 = givenGenreExists("genre3", 3)

        assertEquals(2, dao.deleteByIds(listOf(1, 3, 4)))
        assertEquals(listOf(genre2), dao.all)
    }

    @Test
    fun deleteSingleEntry() {
        val genre1 = givenGenreExists("genre1", 1)
        val genre2 = givenGenreExists("genre2", 2)
        val genre3 = givenGenreExists("genre3", 3)

        assertEquals(1, dao.deleteByIds(listOf(2)))
        assertEquals(listOf(genre1, genre3), dao.all)
    }

    @Test(expected = JdbcSQLException::class)
    fun doesntInsertSameEntryTwice() {
        givenGenreExists("genre", 1)
        dao.add("genre")
    }

    private fun givenGenreExists(name: String, expectedId: Int): Genre = dao.add(name).apply {
        assertEquals(Genre(expectedId, name), this)
    }
}
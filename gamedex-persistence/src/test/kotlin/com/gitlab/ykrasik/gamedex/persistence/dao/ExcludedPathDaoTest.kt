package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.datamodel.persistence.ExcludedPath
import org.h2.jdbc.JdbcSQLException
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Paths

/**
 * User: ykrasik
 * Date: 06/10/2016
 * Time: 22:30
 */
class ExcludedPathDaoTest : AbstractDaoTest() {
    val dao = ExcludedPathDaoImpl()

    @Test
    fun insertAndCheckContains() {
        givenExcludedPathExists("path/to/thing", 1)
        assertTrue(Paths.get("path/to/thing") in dao)
        assertFalse(Paths.get("path/to/thing2") in dao)
    }

    @Test
    fun returnAllExistingEntries() {
        val path1 = givenExcludedPathExists("path1", 1)
        val path2 = givenExcludedPathExists("path2", 2)
        val path3 = givenExcludedPathExists("path3", 3)

        assertEquals(listOf(path1, path2, path3), dao.all)
    }

    @Test
    fun deleteEntry() {
        val path1 = givenExcludedPathExists("path1", 1)
        val path2 = givenExcludedPathExists("path2", 2)
        val path3 = givenExcludedPathExists("path3", 3)

        dao.delete(2)
        assertEquals(listOf(path1, path3), dao.all)
    }

    @Test(expected = IllegalArgumentException::class)
    fun throwsExceptionOnDeleteNonExistingEntry() {
        val path1 = givenExcludedPathExists("path1", 1)
        val path2 = givenExcludedPathExists("path2", 2)
        val path3 = givenExcludedPathExists("path3", 3)

        dao.delete(4)
    }

    @Test(expected = JdbcSQLException::class)
    fun doesntInsertSameEntryTwice() {
        givenExcludedPathExists("path", 1)
        dao.add(Paths.get("path"))
    }

    private fun givenExcludedPathExists(rawPath: String, expectedId: Int): ExcludedPath {
        val path = Paths.get(rawPath)
        return dao.add(path).apply {
            assertEquals(ExcludedPath(expectedId, path), this)
        }
    }
}
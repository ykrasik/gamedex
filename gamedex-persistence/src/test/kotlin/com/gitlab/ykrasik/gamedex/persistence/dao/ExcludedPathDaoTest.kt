package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.toId
import com.github.ykrasik.gamedex.common.toPath
import com.github.ykrasik.gamedex.datamodel.persistence.ExcludedPath
import org.h2.jdbc.JdbcSQLException

/**
 * User: ykrasik
 * Date: 06/10/2016
 * Time: 22:30
 */
class ExcludedPathDaoTest : DaoTest() {
    val dao = excludedPathDao

    init {
        "ExcludedPathDao" should {
            "insert and check that the path exists" {
                givenExcludedPathExists(1, "path/to/thing")
                dao.exists("path/to/thing".toPath()) shouldBe true
                dao.exists("path/to/thing2".toPath()) shouldBe false
            }

            "return all existing paths" {
                val path1 = givenExcludedPathExists(1, "path1")
                val path2 = givenExcludedPathExists(2, "path2")
                val path3 = givenExcludedPathExists(3, "path3")

                dao.all shouldBe listOf(path1, path2, path3)
            }

            "delete an existing path" {
                val path1 = givenExcludedPathExists(1, "path1")
                val path2 = givenExcludedPathExists(2, "path2")
                val path3 = givenExcludedPathExists(3, "path3")

                dao.delete(path2)
                dao.all shouldBe listOf(path1, path3)
            }

            "throw an exception when trying to delete a path that doesn't exist" {
                givenExcludedPathExists(1, "path1")

                shouldThrow<IllegalArgumentException> {
                    val invalidPath = ExcludedPath(2.toId(), "".toPath())
                    dao.delete(invalidPath)
                }
            }

            "throw an exception when trying to insert the same path twice" {
                givenExcludedPathExists(1, "path")

                shouldThrow<JdbcSQLException> {
                    dao.add("path".toPath())
                }
            }
        }
    }
}
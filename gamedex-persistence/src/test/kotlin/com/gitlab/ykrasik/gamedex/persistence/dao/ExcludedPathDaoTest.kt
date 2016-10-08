package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.datamodel.persistence.ExcludedPath
import org.h2.jdbc.JdbcSQLException
import java.nio.file.Paths

/**
 * User: ykrasik
 * Date: 06/10/2016
 * Time: 22:30
 */
class ExcludedPathDaoTest : BaseDaoTest() {
    val dao = ExcludedPathDaoImpl()

    init {
        "ExcludedPathDao" should {
            "insert and check that the path is contained" {
                givenExcludedPathExists("path/to/thing", 1)
                dao.contains(Paths.get("path/to/thing")) shouldBe true
                dao.contains(Paths.get("path/to/thing2")) shouldBe false
            }

            "return all existing paths" {
                val path1 = givenExcludedPathExists("path1", 1)
                val path2 = givenExcludedPathExists("path2", 2)
                val path3 = givenExcludedPathExists("path3", 3)

                dao.all shouldBe listOf(path1, path2, path3)
            }

            "delete an existing path" {
                val path1 = givenExcludedPathExists("path1", 1)
                val path2 = givenExcludedPathExists("path2", 2)
                val path3 = givenExcludedPathExists("path3", 3)

                dao.delete(2)
                dao.all shouldBe listOf(path1, path3)
            }

            "throw an exception when trying to delete a path that doesn't exist" {
                givenExcludedPathExists("path1", 1)
                givenExcludedPathExists("path2", 2)
                givenExcludedPathExists("path3", 3)

                shouldThrow<IllegalArgumentException> {
                    dao.delete(4)
                }
            }

            "throw an exception when trying to insert the same path twice" {
                givenExcludedPathExists("path", 1)
                shouldThrow<JdbcSQLException> {
                    dao.add(Paths.get("path"))
                }
            }
        }
    }


    private fun givenExcludedPathExists(rawPath: String, expectedId: Int): ExcludedPath {
        val path = Paths.get(rawPath)
        return dao.add(path).apply {
            this shouldBe ExcludedPath(expectedId, path)
        }
    }
}
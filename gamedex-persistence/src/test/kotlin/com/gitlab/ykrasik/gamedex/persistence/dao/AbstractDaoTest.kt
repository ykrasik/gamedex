package com.gitlab.ykrasik.gamedex.persistence.dao

import com.gitlab.ykrasik.gamedex.persistence.TestDbInitializer
import org.junit.Before

/**
 * User: ykrasik
 * Date: 06/10/2016
 * Time: 20:55
 */
abstract class AbstractDaoTest {
    @Before
    fun beforeEach() {
        TestDbInitializer.reload()
    }
}
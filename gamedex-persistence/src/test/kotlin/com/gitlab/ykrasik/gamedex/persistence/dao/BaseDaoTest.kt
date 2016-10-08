package com.gitlab.ykrasik.gamedex.persistence.dao

import com.gitlab.ykrasik.gamedex.persistence.TestDbInitializer
import io.kotlintest.specs.WordSpec

/**
 * User: ykrasik
 * Date: 06/10/2016
 * Time: 20:55
 */
abstract class BaseDaoTest : WordSpec() {
    override fun beforeEach() {
        TestDbInitializer.reload()
    }
}
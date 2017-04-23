package com.gitlab.ykrasik.gamedex.provider.igdb

import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.specs.StringSpec
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 04/02/2017
 * Time: 10:56
 */
class IgdbReleaseDateTest : StringSpec() {
    init {
        "Parse category 0" {
            releaseDate(0, "2016-Oct-25").toLocalDate() shouldBe LocalDate.parse("2016-10-25")
        }

        "Parse category 1" {
            releaseDate(1, "2016-Oct").toLocalDate() shouldBe LocalDate.parse("2016-10")
        }

        "Parse category 2" {
            releaseDate(2, "2016").toLocalDate() shouldBe LocalDate.parse("2016")
        }

        "Parse category 3" {
            releaseDate(3, "2016-Q1").toLocalDate() shouldBe LocalDate.parse("2016")
        }

        "Parse category 4" {
            releaseDate(4, "2016-Q2").toLocalDate() shouldBe LocalDate.parse("2016")
        }

        "Parse category 5" {
            releaseDate(5, "2016-Q3").toLocalDate() shouldBe LocalDate.parse("2016")
        }

        "Parse category 6" {
            releaseDate(6, "2016-Q4").toLocalDate() shouldBe LocalDate.parse("2016")
        }

        "Parse category 7" {
            releaseDate(7, "TBD").toLocalDate() shouldBe null
        }

        "Fail on invalid category" {
            shouldThrow<IllegalArgumentException> {
                releaseDate(8, "TBD").toLocalDate()
            }
        }
    }

    private fun releaseDate(category: Int, str: String) = IgdbClient.ReleaseDate(platform = 0, category = category, human = str)
}
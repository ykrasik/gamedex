package com.gitlab.ykrasik.gamedex.persistence

/**
 * User: ykrasik
 * Date: 26/05/2016
 * Time: 15:56
 */
data class PersistenceConfig(
    val dbUrl: String,
    val driver: String,
    val user: String,
    val password: String
) {
    companion object {
        val test: PersistenceConfig get() = PersistenceConfig(
            dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
    }
}
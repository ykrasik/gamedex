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
)
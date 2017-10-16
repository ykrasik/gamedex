package com.gitlab.ykrasik.gamedex.persistence

import com.typesafe.config.Config

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
        @Suppress("NAME_SHADOWING")
        operator fun invoke(config: Config): PersistenceConfig = config.getConfig("gameDex.persistence").let { config ->
            PersistenceConfig(
                dbUrl = config.getString("dbUrl"),
                driver = config.getString("driver"),
                user = config.getString("user"),
                password = config.getString("password")
            )
        }
    }
}
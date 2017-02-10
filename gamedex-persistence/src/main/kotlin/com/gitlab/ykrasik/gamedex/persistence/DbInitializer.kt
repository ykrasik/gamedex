package com.gitlab.ykrasik.gamedex.persistence

import com.gitlab.ykrasik.gamedex.common.util.logger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/10/2016
 * Time: 18:20
 */
@Singleton
open class DbInitializer @Inject constructor(private val config: PersistenceConfig) {
    private val log by logger()

    init {
        log.debug { "Connection url: ${config.dbUrl}" }
        Database.connect(config.dbUrl, config.driver, config.user, config.password)
    }

    fun create() = transaction {
        org.jetbrains.exposed.sql.SchemaUtils.create(
            Libraries, Games, Images
        )
    }
}
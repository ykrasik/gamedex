package com.gitlab.ykrasik.gamedex.persistence

import com.github.ykrasik.gamedex.common.util.logger
import com.google.common.annotations.VisibleForTesting
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/10/2016
 * Time: 18:20
 */
@Singleton
class DbInitializer @Inject constructor(private val config: PersistenceConfig) {
    private val log by logger()

    private val flyway = Flyway().let {
        it.setDataSource(config.dbUrl, config.user, config.password)
        it.isBaselineOnMigrate = true
        it.isCleanDisabled = true
        it
    }

    fun init() {
        connect()
        migrate()
    }

    fun connect() {
        log.debug { "Connection url: ${config.dbUrl}" }
        Database.connect(config.dbUrl, config.driver, config.user, config.password)
    }

    fun migrate() {
        flyway.migrate()
    }

    @VisibleForTesting
    internal fun enableDestroy() {
        flyway.isCleanDisabled = false
    }

    @VisibleForTesting
    internal fun destroy() {
        flyway.clean()
    }
}
package com.gitlab.ykrasik.gamedex.persistence

import org.jetbrains.exposed.sql.transactions.transaction

/**
 * User: ykrasik
 * Date: 06/10/2016
 * Time: 21:18
 */
object TestDbInitializer {
    private val initializer: DbInitializer = DbInitializer(PersistenceConfig(
        dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    ))

    init {
        initializer.connect()
        initializer.enableDestroy()
    }

    fun init() {
        initializer.migrate()
    }

    fun destroy() {
        initializer.destroy()
    }

    fun reload() {
        destroy()
        init()
    }

    fun disableReferentialIntegrity(): Unit = transaction {
        exec("SET REFERENTIAL_INTEGRITY false")
    }

    fun enableReferentialIntegrity(): Unit = transaction {
        exec("SET REFERENTIAL_INTEGRITY true")
    }

    fun withoutReferentialIntegrity(f: () -> Unit): Unit = try {
        disableReferentialIntegrity()
        f()
    } finally {
        enableReferentialIntegrity()
    }
}
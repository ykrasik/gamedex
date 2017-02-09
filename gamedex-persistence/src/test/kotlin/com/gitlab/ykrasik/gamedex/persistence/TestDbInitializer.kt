package com.gitlab.ykrasik.gamedex.persistence

import org.jetbrains.exposed.sql.transactions.transaction

object TestDbInitializer : DbInitializer(PersistenceConfig(
    dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
    driver = "org.h2.Driver",
    user = "sa",
    password = ""
)) {

    fun drop() = transaction {
        org.jetbrains.exposed.sql.SchemaUtils.drop(
            Libraries, Games, Images
        )
    }

    fun reload() {
        drop()
        create()
    }
}
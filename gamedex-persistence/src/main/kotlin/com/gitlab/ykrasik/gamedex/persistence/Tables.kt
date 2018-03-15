package com.gitlab.ykrasik.gamedex.persistence

import com.gitlab.ykrasik.gamedex.util.kb
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.CurrentDateTime
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 08:51
 */
internal object Libraries : IntIdTable() {
    val path = varchar("path", 255).uniqueIndex()
    val data = varchar("data", 1.kb)
}

internal object Games : IntIdTable() {
    val libraryId = reference("library_id", Libraries, onDelete = ReferenceOption.CASCADE)
    val path = varchar("path", 255)
    val providerData = varchar("provider_data", 32.kb)
    val userData = varchar("user_data", 16.kb).nullable()
    val updateDate = datetime("update_date")
    val createDate = datetime("create_date").defaultExpression(CurrentDateTime())

    init {
        // Path is unique-per-libraryId
        index(true, path, libraryId)
    }
}

internal object Images : IntIdTable() {
    val gameId = reference("game_id", Games, onDelete = ReferenceOption.CASCADE)
    val url = varchar("url", length = 255).uniqueIndex()
    val bytes = blob("bytes")
}

// This doesn't exist by default in exposed
fun <T : Table> T.updateAll(body: T.(UpdateStatement) -> Unit): Int {
    val query = UpdateStatement(this, null, null)
    body(query)
    return query.execute(TransactionManager.current())!!
}
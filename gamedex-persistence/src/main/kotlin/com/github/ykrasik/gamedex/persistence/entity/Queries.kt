package com.github.ykrasik.gamedex.persistence.entity

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.*
import java.sql.Blob
import javax.sql.rowset.serial.SerialBlob

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 09:12
 */

fun IntIdTable.selectById(id: Int): ResultRow {
    val table = this
    return select { table.id.eq(EntityID(id, table)) }.first()
}

inline fun <T : Table> T.selectBy(selector: (T) -> Op<Boolean>): Query = select { selector(this@selectBy) }

fun IntIdTable.deleteById(id: Int): Int {
    val table = this
    return deleteWhere { table.id.eq(EntityID(id, table)) }
}

inline fun <T : Table> T.deleteBy(crossinline selector: (T) -> Op<Boolean>): Int = deleteWhere { selector(this@deleteBy) }

val Blob.bytes: ByteArray get() = getBytes(0, length().toInt())

fun ByteArray.toBlob(): Blob = SerialBlob(this)

inline fun IntIdTable.id(id: Int): EntityID<Int> = EntityID(id, this)
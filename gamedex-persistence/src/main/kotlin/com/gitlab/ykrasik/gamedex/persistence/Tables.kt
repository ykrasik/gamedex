package com.gitlab.ykrasik.gamedex.persistence

import com.github.ykrasik.gamedex.common.util.kb
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 08:51
 */
internal object Libraries : IntIdTable() {
    val path = varchar("path", 255).uniqueIndex()
    val data = varchar("data", 16.kb)
}

internal object Games : IntIdTable() {
    val libraryId = reference("library_id", Libraries, onDelete = ReferenceOption.CASCADE)
    val path = varchar("path", 255).uniqueIndex()
    val lastModified = datetime("last_modified")
    val data = varchar("data", 16.kb)
}

internal object Images : IntIdTable() {
    val gameId = optReference("game_id", Games, onDelete = ReferenceOption.CASCADE)
    val type = enumeration("type", GameImageType::class.java)
    val url = varchar("url", length = 2084) // FIXME: check for uniqueness
    val bytes = blob("bytes").nullable()
}

internal enum class GameImageType {
    Thumbnail,
    Poster,
    Screenshot
}
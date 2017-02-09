package com.gitlab.ykrasik.gamedex.persistence

import com.github.ykrasik.gamedex.common.util.kb
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

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

internal object Images : Table("") {
    val gameId = reference("game_id", Games, onDelete = ReferenceOption.CASCADE).primaryKey()

    val thumbnail = image("thumbnail")
    val thumbnailUrl = imageUrl("thumbnail_url")

    val poster = image("poster")
    val posterUrl = imageUrl("poster_url")

    val screenshot1 = image("screenshot1")
    val screenshot1Url = imageUrl("screenshot1_url")

    val screenshot2 = image("screenshot2")
    val screenshot2Url = imageUrl("screenshot2_url")

    val screenshot3 = image("screenshot3")
    val screenshot3Url = imageUrl("screenshot3_url")

    val screenshot4 = image("screenshot4")
    val screenshot4Url = imageUrl("screenshot4_url")

    val screenshot5 = image("screenshot5")
    val screenshot5Url = imageUrl("screenshot5_url")

    val screenshot6 = image("screenshot6")
    val screenshot6Url = imageUrl("screenshot6_url")

    val screenshot7 = image("screenshot7")
    val screenshot7Url = imageUrl("screenshot7_url")

    val screenshot8 = image("screenshot8")
    val screenshot8Url = imageUrl("screenshot8_url")

    val screenshot9 = image("screenshot9")
    val screenshot9Url = imageUrl("screenshot9_url")

    val screenshot10 = image("screenshot10")
    val screenshot10Url = imageUrl("screenshot10_url")

    private fun image(name: String) = blob(name).nullable()
    private fun imageUrl(name: String) = varchar(name, 256).nullable()
}
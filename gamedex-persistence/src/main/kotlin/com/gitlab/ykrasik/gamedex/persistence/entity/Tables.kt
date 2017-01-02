package com.gitlab.ykrasik.gamedex.persistence.entity

import com.github.ykrasik.gamedex.datamodel.GamePlatform
import org.jetbrains.exposed.sql.Table

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 08:51
 */
// TODO: Why doesn't extending IntIdTable work?

object Libraries : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val path = varchar("path", 255).uniqueIndex()
    val name = varchar("name", 255)
    val platform = enumeration("platform", GamePlatform::class.java)
}

object Games : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val path = varchar("path", 255).uniqueIndex()
    val lastModified = datetime("last_modified")
    val library = reference("library_id", Libraries.id)

    val name = varchar("name", 255)
    val description = varchar("description", 255).nullable()
    val releaseDate = date("release_date").nullable()

    val criticScore = decimal("critic_score", 9, 1).nullable()
    val userScore = decimal("user_score", 9, 1).nullable()

    val providerData = varchar("provider_data", 8192)
}

object Images : Table("") {
    val game = reference("game_id", Games.id).primaryKey()

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
    val screenshot4Ulr = imageUrl("screenshot4_url")

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

object Genres : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val name = varchar("name", 255).uniqueIndex()
}

object GameGenres : Table("game_genres") {
    val id = integer("id").autoIncrement().primaryKey()
    val game = reference("game_id", Games.id)
    val genre = reference("genre_id", Genres.id)
}

object ExcludedPaths : Table("excluded_paths") {
    val id = integer("id").autoIncrement().primaryKey()
    val path = varchar("path", 255).uniqueIndex()
}
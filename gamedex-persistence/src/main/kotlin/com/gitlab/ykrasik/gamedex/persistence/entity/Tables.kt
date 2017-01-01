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

    val thumbnail = blob("thumbnail").nullable()
    val poster = blob("poster").nullable()

    val screenshot1 = blob("screenshot1").nullable()
    val screenshot2 = blob("screenshot2").nullable()
    val screenshot3 = blob("screenshot3").nullable()
    val screenshot4 = blob("screenshot4").nullable()
    val screenshot5 = blob("screenshot5").nullable()
    val screenshot6 = blob("screenshot6").nullable()
    val screenshot7 = blob("screenshot7").nullable()
    val screenshot8 = blob("screenshot8").nullable()
    val screenshot9 = blob("screenshot9").nullable()
    val screenshot10 = blob("screenshot10").nullable()

    val imageData = varchar("image_data", 8192)
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
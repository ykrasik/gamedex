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
    val name = varchar("name", 255)

    val releaseDate = date("release_date").nullable()
    val description = varchar("description", 255).nullable()

    val criticScore = decimal("critic_score", 9, 1).nullable()
    val userScore = decimal("user_score", 9, 1).nullable()

    // TODO: Remove this, make this a one to many.
    val thumbnail = blob("thumbnail").nullable()
    val poster = blob("poster").nullable()

    val lastModified = datetime("last_modified")

    val providerSpecificData = varchar("provider_specific_data", 64000)

    val library = reference("library_id", Libraries.id)

    // Extensions
    // TODO: Remove this, make this a one to many.
    val withoutBlobs = columns - arrayOf(thumbnail, poster)
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
package com.github.ykrasik.gamedex.persistence.entity

import com.github.ykrasik.gamedex.datamodel.GamePlatform
import org.jetbrains.exposed.dao.IntIdTable

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 08:51
 */

object Libraries : IntIdTable() {
//    val id = integer("id").autoIncrement().primaryKey()
    val path = varchar("path", 255).uniqueIndex()
    val name = varchar("name", 255)
    val platform = enumeration("platform", GamePlatform::class.java)
}

object Games : IntIdTable() {
//    val id = integer("id").autoIncrement().primaryKey()
    val path = varchar("path", 255).uniqueIndex()
    val name = varchar("name", 255)

    val releaseDate = date("release_date").nullable()
    val description = varchar("description", 255).nullable()

    val criticScore = decimal("critic_score", 9, 1).nullable()
    val userScore = decimal("user_score", 9, 1).nullable()

    val metacriticUrl = varchar("metacritic_url", 255)
    val giantBombUrl = varchar("giantbomb_url", 255).nullable()

    val thumbnail = blob("thumbnail").nullable()
    val poster = blob("poster").nullable()

    val lastModified = datetime("last_modified")

    val library = reference("library_id", Libraries.id)

    // Extensions

    val withoutBlobs = columns - arrayOf(thumbnail, poster)
}

object Genres : IntIdTable() {
//    val id = integer("id").autoIncrement().primaryKey()
    val name = varchar("name", 255)
}

object GameGenres : IntIdTable() {
//    val id = integer("id").autoIncrement().primaryKey()
    val game = reference("game_id", Games.id)
    val genre = reference("genre_id", Genres.id)
}

object ExcludedPaths : IntIdTable() {
//    val id = integer("id").autoIncrement().primaryKey()
    val path = Libraries.varchar("path", 255).uniqueIndex()
}
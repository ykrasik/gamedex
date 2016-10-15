package com.github.ykrasik.gamedex.datamodel

import com.github.ykrasik.gamedex.common.Id
import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.nio.file.Path

/**
 * User: ykrasik
 * Date: 26/05/2016
 * Time: 15:32
 */
data class Game(
    val id: Id<Game>,
    override val path: Path,

    val name: String,
    val description: String?,
    val releaseDate: LocalDate?,

    val criticScore: Double?,
    val userScore: Double?,

    val lastModified: DateTime,

    // TODO: Do something about these fields, maybe save as a list of ProviderExtensions?
    val metacriticUrl: String,
    val giantBombUrl: String?,

    val genres: List<Genre>,
    val library: Library  // TODO: Is this needed? It holds no interesting information, can probably save just the Id.
) : HasPath {

    override fun toString() = "Game(id = $id, name = $name, path = $path)"
}
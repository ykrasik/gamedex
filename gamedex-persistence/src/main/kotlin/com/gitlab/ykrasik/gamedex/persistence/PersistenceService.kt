package com.gitlab.ykrasik.gamedex.persistence

import com.github.ykrasik.gamedex.common.d
import com.github.ykrasik.gamedex.common.logger
import com.gitlab.ykrasik.gamedex.persistence.dao.ExcludedPathDao
import com.gitlab.ykrasik.gamedex.persistence.dao.GameDao
import com.gitlab.ykrasik.gamedex.persistence.dao.GenreDao
import com.gitlab.ykrasik.gamedex.persistence.dao.LibraryDao
import com.gitlab.ykrasik.gamedex.persistence.entity.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 15:10
 */
interface PersistenceService {
    val games: GameDao
    val genres: GenreDao
    val libraries: LibraryDao
    val excludedPaths: ExcludedPathDao
}

@Singleton
class PersistenceServiceImpl @Inject constructor(
    private val config: PersistenceConfig,
    override val games: GameDao,
    override val genres: GenreDao,
    override val libraries: LibraryDao,
    override val excludedPaths: ExcludedPathDao
) : PersistenceService {
    private val log by logger()

    init {
        log.d { "Connection url: ${config.dbUrl}" }
        Database.connect(config.dbUrl, config.driver, config.user, config.password)

        // TODO: Use Db evolutions!!!
        log.debug("Creating db...")
        SchemaUtils.create(Libraries, Games, Genres, GameGenres, ExcludedPaths)
    }
}
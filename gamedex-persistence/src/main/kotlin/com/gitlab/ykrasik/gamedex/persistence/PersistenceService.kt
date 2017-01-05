package com.gitlab.ykrasik.gamedex.persistence

import com.github.ykrasik.gamedex.common.TimeProvider
import com.gitlab.ykrasik.gamedex.persistence.dao.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 15:10
 */
// FIXME: There is no need for individual DAO objects. It's ok for namespace segregation, but the whole code can be contained in this class.
interface PersistenceService {
    val games: GameDao
    val images: ImageDao
    val libraries: LibraryDao
    val excludedPaths: ExcludedPathDao
}

@Singleton
class PersistenceServiceImpl @Inject constructor(
    initializer: DbInitializer,
    timeProvider: TimeProvider
) : PersistenceService {

    override val games = GameDaoImpl(timeProvider)
    override val images = ImageDaoImpl()
    override val libraries = LibraryDaoImpl()
    override val excludedPaths = ExcludedPathDaoImpl()

    init {
        initializer.init()
    }
}
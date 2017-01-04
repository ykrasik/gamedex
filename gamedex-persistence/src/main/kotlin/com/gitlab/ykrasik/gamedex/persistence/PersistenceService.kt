package com.gitlab.ykrasik.gamedex.persistence

import com.gitlab.ykrasik.gamedex.persistence.dao.ExcludedPathDao
import com.gitlab.ykrasik.gamedex.persistence.dao.GameDao
import com.gitlab.ykrasik.gamedex.persistence.dao.LibraryDao
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
    val libraries: LibraryDao
    val excludedPaths: ExcludedPathDao
}

@Singleton
class PersistenceServiceImpl @Inject constructor(
    initializer: DbInitializer,
    override val games: GameDao,
    override val libraries: LibraryDao,
    override val excludedPaths: ExcludedPathDao
) : PersistenceService {

    init {
        initializer.init()
    }
}
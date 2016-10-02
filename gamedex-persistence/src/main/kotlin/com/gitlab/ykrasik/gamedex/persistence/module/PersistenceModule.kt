package com.gitlab.ykrasik.gamedex.persistence.module

import com.gitlab.ykrasik.gamedex.persistence.PersistenceConfig
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.persistence.PersistenceServiceImpl
import com.gitlab.ykrasik.gamedex.persistence.dao.*
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.typesafe.config.Config
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 15:22
 */
class PersistenceModule : AbstractModule() {
    override fun configure() {
        bind(PersistenceService::class.java).to(PersistenceServiceImpl::class.java)

        bind(GameDao::class.java).to(GameDaoImpl::class.java)
        bind(GenreDao::class.java).to(GenreDaoImpl::class.java)
        bind(GameGenreDao::class.java).to(GameGenreDaoImpl::class.java)
        bind(LibraryDao::class.java).to(LibraryDaoImpl::class.java)
        bind(ExcludedPathDao::class.java).to(ExcludedPathDaoImpl::class.java)
    }

    @Provides
    @Singleton
    fun persistenceConfig(config: Config): PersistenceConfig = config.getConfig("gameDex.persistence").let { config ->
        PersistenceConfig(
            dbUrl = config.getString("dbUrl"),
            driver = config.getString("driver"),
            user = config.getString("user"),
            password = config.getString("password")
        )
    }
}
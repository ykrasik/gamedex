package com.gitlab.ykrasik.gamedex.persistence.module

import com.gitlab.ykrasik.gamedex.persistence.PersistenceConfig
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.persistence.PersistenceServiceImpl
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.typesafe.config.Config
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 15:22
 */
object PersistenceModule : AbstractModule() {
    override fun configure() {
        bind(PersistenceService::class.java).to(PersistenceServiceImpl::class.java)
    }

    @Provides
    @Singleton
    fun persistenceConfig(config: Config) = PersistenceConfig(config)
}
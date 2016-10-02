package com.github.ykrasik.gamedex.persistence.module

import com.github.ykrasik.gamedex.persistence.PersistenceService
import com.github.ykrasik.gamedex.persistence.PersistenceServiceImpl
import com.github.ykrasik.gamedex.persistence.config.PersistenceConfig
import com.github.ykrasik.gamedex.persistence.config.PersistenceConfigImpl
import com.google.inject.AbstractModule

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 09:53
 */
class PersistenceModule : AbstractModule() {
    override fun configure() {
        bind(PersistenceConfig::class.java).to(PersistenceConfigImpl::class.java)
        bind(PersistenceService::class.java).to(PersistenceServiceImpl::class.java)
    }
}
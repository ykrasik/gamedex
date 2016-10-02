package com.github.ykrasik.gamedex.persistence.config

import com.github.ykrasik.gamedex.common.BaseConfig
import com.typesafe.config.Config
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 26/05/2016
 * Time: 15:56
 */
interface PersistenceConfig {
    val dbUrl: String
}

@Singleton
class PersistenceConfigImpl @Inject constructor(c: Config) : BaseConfig(c, "db"), PersistenceConfig {
    override val dbUrl = config.getString("url")
}
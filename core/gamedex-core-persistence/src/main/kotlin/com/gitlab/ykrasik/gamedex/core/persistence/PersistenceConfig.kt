/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.core.persistence

import com.google.inject.ProvidedBy
import com.typesafe.config.Config
import io.github.config4k.extract
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 26/05/2016
 * Time: 15:56
 */
@ProvidedBy(PersistenceConfigProvider::class)
data class PersistenceConfig(
    val dbUrl: String,
    val driver: String,
    val user: String,
    val password: String
)

@Singleton
class PersistenceConfigProvider @Inject constructor(config: Config) : Provider<PersistenceConfig> {
    private val config = config.extract<PersistenceConfig>("gameDex.persistence")
    override fun get() = config
}
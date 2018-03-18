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
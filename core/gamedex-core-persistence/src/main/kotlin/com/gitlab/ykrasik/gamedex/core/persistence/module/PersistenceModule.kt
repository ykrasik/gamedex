/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.persistence.module

import com.gitlab.ykrasik.gamedex.core.persistence.PersistenceConfig
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.typesafe.config.Config
import io.github.config4k.extract
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 16/02/2019
 * Time: 12:44
 */
object PersistenceModule : AbstractModule() {
    val configFile = "/com/gitlab/ykrasik/gamedex/core/persistence/persistence.conf"

    @Provides
    @Singleton
    fun persistenceConfig(config: Config): PersistenceConfig = config.extract("gameDex.persistence")
}
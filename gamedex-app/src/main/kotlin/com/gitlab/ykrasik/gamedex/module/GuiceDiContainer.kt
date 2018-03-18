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

package com.gitlab.ykrasik.gamedex.module

import com.gitlab.ykrasik.gamedex.persistence.module.PersistenceModule
import com.google.inject.Guice
import com.google.inject.Module
import com.google.inject.Stage
import tornadofx.DIContainer
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 02/04/2017
 * Time: 11:58
 */
class GuiceDiContainer(modules: List<Module> = GuiceDiContainer.defaultModules) : DIContainer {
    private val injector = Guice.createInjector(Stage.PRODUCTION, modules)

    override fun <T : Any> getInstance(type: KClass<T>): T = injector.getInstance(type.java)

    companion object {
        val defaultModules = listOf(
            AppModule, ConfigModule, PersistenceModule, ProviderScannerModule
        )
    }
}
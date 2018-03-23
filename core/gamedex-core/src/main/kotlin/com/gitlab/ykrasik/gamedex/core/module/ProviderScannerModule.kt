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

package com.gitlab.ykrasik.gamedex.core.module

import com.gitlab.ykrasik.gamedex.core.util.ClassPathScanner
import com.gitlab.ykrasik.gamedex.provider.ProviderModule
import com.google.inject.AbstractModule

/**
 * User: ykrasik
 * Date: 30/05/2017
 * Time: 22:14
 */
object ProviderScannerModule : AbstractModule() {
    override fun configure() {
        val providerModules = scanProviderModules()
        providerModules.forEach { install(it) }
    }

    private fun scanProviderModules(): List<AbstractModule> {
        val classes = ClassPathScanner.scanSubTypes("", ProviderModule::class)
        return classes.map { it.kotlin.objectInstance!! }
    }
}
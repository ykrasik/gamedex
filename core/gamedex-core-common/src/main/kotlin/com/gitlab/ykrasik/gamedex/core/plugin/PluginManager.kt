/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.plugin

import com.gitlab.ykrasik.gamedex.plugin.Plugin
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 26/03/2018
 * Time: 09:57
 */
interface PluginManager {
    val allPlugins: List<Plugin>
    val compatiblePlugins: List<Plugin>

    fun <T : Any> getImplementations(klass: KClass<T>): List<T>
}
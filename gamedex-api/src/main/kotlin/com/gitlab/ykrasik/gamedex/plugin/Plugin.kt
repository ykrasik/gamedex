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

package com.gitlab.ykrasik.gamedex.plugin

import com.gitlab.ykrasik.gamedex.Version
import com.gitlab.ykrasik.gamedex.util.fromJson
import com.gitlab.ykrasik.gamedex.util.getResourceAsByteArray
import com.google.inject.AbstractModule
import com.google.inject.Module
import org.joda.time.DateTime

/**
 * User: ykrasik
 * Date: 14/02/2019
 * Time: 08:06
 */
typealias PluginId = String

interface Plugin {
    val descriptor: PluginDescriptor
    val module: Module
}

data class PluginDescriptor(
    val id: PluginId,
    val author: String,
    val version: String,
    val description: String? = null,
    val buildDate: DateTime? = null,
    val commitHash: String? = null,
    val commitDate: DateTime? = null,
    val apiDependencies: Map<ApplicationApi, Int> = emptyMap(),
) {
    val appVersion by lazy {
        Version(
            version = version,
            buildDate = buildDate,
            commitHash = commitHash,
            commitDate = commitDate
        )
    }
}

val Plugin.id get() = descriptor.id
val Plugin.author get() = descriptor.author
val Plugin.version get() = descriptor.version
val Plugin.description get() = descriptor.description
val Plugin.buildDate get() = descriptor.buildDate
val Plugin.commitHash get() = descriptor.commitHash
val Plugin.commitDate get() = descriptor.commitDate
val Plugin.apiDependencies get() = descriptor.apiDependencies
val Plugin.fullyQualifiedName get() = "$id@$version"

abstract class DefaultPlugin : AbstractModule(), Plugin {
    override val module get() = this

    override fun toString() = fullyQualifiedName

    protected fun readPluginDescriptor(path: String): PluginDescriptor =
        getResourceAsByteArray(path).fromJson()
}

enum class ApplicationApi {
    Plugin,
    Provider
}
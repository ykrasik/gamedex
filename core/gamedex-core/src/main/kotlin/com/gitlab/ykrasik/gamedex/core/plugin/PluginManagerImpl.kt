/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.core.applicationApiVersions
import com.gitlab.ykrasik.gamedex.core.env
import com.gitlab.ykrasik.gamedex.plugin.*
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.time
import com.google.inject.Injector
import io.github.classgraph.ClassGraph
import java.io.File
import java.util.jar.Manifest
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 14/02/2019
 * Time: 09:08
 */
class PluginManagerImpl(injector: Injector, pluginScanners: List<PluginScanner>) : PluginManager {
    private val log = logger()

    init {
        log.info("Environment: $env")
    }

    private val allInternalPlugins: Map<PluginId, InternalPlugin> = log.time("Detecting plugins...", { time, plugins -> "${plugins.size} plugins in $time" }) {
        val results = pluginScanners.map { it.scan() }
        results.fold(mapOf()) { acc, pluginClasses ->
            acc + pluginClasses.mapNotNull { (pluginClass, classLoader) ->
                runCatching {
                    val plugin = pluginClass.kotlin.objectInstance as Plugin
                    log.info("Detected plugin: ${plugin.fullyQualifiedName}")
                    val childInjector = injector.createChildInjector(plugin.module)
                    plugin.id to InternalPlugin(plugin, classLoader, childInjector)
                }.getOrElse { e ->
                    log.error("[$pluginClass] Ignoring due to error:", e)
                    null
                }
            }
        }
    }

    private val compatibleInternalPlugins = allInternalPlugins.filter { (_, plugin) ->
        // If plugin doesn't declare a PluginApi, default to 0
        val pluginApiVersions = plugin.plugin.apiDependencies.let { apiDependencies ->
            apiDependencies + (ApplicationApi.Plugin to (apiDependencies[ApplicationApi.Plugin] ?: 0))
        }

        val incompatiblePluginApis = pluginApiVersions.filter { (api, pluginApiVersion) ->
            val applicationApiVersion = applicationApiVersions.getValue(api)
            pluginApiVersion != applicationApiVersion
        }

        incompatiblePluginApis.forEach { (api, pluginApiVersion) ->
            log.warn("Incompatible plugin: ${plugin.plugin.fullyQualifiedName}. API=$api, PluginApiVersion=$pluginApiVersion, ApplicationApiVersion=${applicationApiVersions[api]}")
        }

        incompatiblePluginApis.isEmpty()
    }

    override val allPlugins = allInternalPlugins.map { it.value.plugin }
    override val compatiblePlugins = compatibleInternalPlugins.map { it.value.plugin }

    override fun <T : Any> getImplementations(klass: KClass<T>) = compatibleInternalPlugins.mapNotNull { (_, plugin) ->
        runCatching {
            plugin.injector.getInstance(klass.java)
        }.getOrNull()
    }.apply {
        log.debug("Found $size implementations of ${klass.qualifiedName} in plugins.")
    }

    data class InternalPlugin(
        val plugin: Plugin,
        val classLoader: ClassLoader,
        val injector: Injector
    )
}

interface PluginScanner {
    fun scan(): List<Pair<Class<out Plugin>, ClassLoader>>
}

class DirectoryPluginScanner(private val pluginsDir: String = "plugins") : PluginScanner {
    private val log = logger()

    @Suppress("UNCHECKED_CAST")
    override fun scan(): List<Pair<Class<out Plugin>, ClassLoader>> {
        val files = File(pluginsDir).listFiles()
        if (files == null) {
            log.error("Plugin directory not found: ${File(pluginsDir).absolutePath}")
            return emptyList()
        }

        return files.mapNotNull { file ->
            if (file.extension != "jar") return@mapNotNull null
            log.trace("[$file] Detecting plugin...")
            runCatching {
                val classLoader = PluginClassLoader(file.toURI().toURL(), javaClass.classLoader)
                val url = classLoader.findResource("META-INF/MANIFEST.MF")
                val manifest = Manifest(url.openStream())
                val pluginClassName = checkNotNull(manifest.mainAttributes.getValue("Plugin-Class")) { "Missing 'Plugin-Class' attribute in manifest." }
                val pluginClass = classLoader.loadClass(pluginClassName)
                log.trace("[$file] Plugin: $pluginClassName")
                pluginClass as Class<out Plugin> to classLoader
            }.getOrElse { e ->
                log.error("[$file] Ignoring due to error:", e)
                null
            }
        }
    }
}

class ClasspathPluginScanner : PluginScanner {
    private val log = logger()

    @Suppress("UNCHECKED_CAST")
    override fun scan() = ClassGraph()
        .acceptJars("*gamedex*")
        .acceptPackages("com.gitlab.ykrasik.gamedex")
        .scan().use { scanResult ->
            val pluginClassInfo = scanResult.getClassesImplementing(Plugin::class.qualifiedName)
            pluginClassInfo.mapNotNull {
                if (it.isAbstract) return@mapNotNull null
                val plugin = it.loadClass() as Class<out Plugin>
                log.trace("[${it.classpathElementFile}] Plugin: ${plugin.canonicalName}")
                plugin to javaClass.classLoader
            }
        }
}
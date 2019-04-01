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

import java.net.URL
import java.net.URLClassLoader

class PluginClassLoader(url: URL, parent: ClassLoader) : URLClassLoader(arrayOf(url), parent) {
    override fun loadClass(className: String): Class<*>? = synchronized(getClassLoadingLock(className)) {
        if (className.startsWith("java.")) {
            return findSystemClass(className)
        }

        val loadedClass = findLoadedClass(className)
        if (loadedClass != null) {
            return loadedClass
        }

        return try {
            findClass(className)
        } catch (_: ClassNotFoundException) {
            super.loadClass(className)
        }
    }

    override fun getResource(name: String): URL? =
        findResource(name) ?: super.getResource(name)
}
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

package com.gitlab.ykrasik.gamedex.util

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueFactory

/**
 * User: ykrasik
 * Date: 01/10/2016
 * Time: 21:56
 */
// TODO: Consider moving this to the app module.
var appConfig: Config = run {
    val configurationFiles = ClassPathScanner.scanResources("com.gitlab.ykrasik.gamedex") {
        it.endsWith(".conf") && it != "application.conf" && it != "reference.conf"
    }

    // Use the default config as a baseline and apply 'withFallback' on it for every custom .conf file encountered.
    configurationFiles.fold(ConfigFactory.load()) { current, url ->
        current.withFallback(ConfigFactory.parseURL(url))
    }
}

fun stringConfig(str: String): ConfigValue = ConfigValueFactory.fromAnyRef(str)
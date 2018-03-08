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
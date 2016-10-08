package com.github.ykrasik.gamedex.common

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 09:09
 */
// TODO: Are global variables like this lazy by default?
val defaultConfig: Config by lazy {
    val configurationFiles = ClassPathScanner.scanPackage("") {
        it.endsWith(".conf") && it != "application.conf" && it != "reference.conf"
    }

    // Use the default config as a baseline and apply 'withFallback' on it for every custom .conf file encountered.
    configurationFiles.fold(ConfigFactory.load()) { current, url ->
        current.withFallback(ConfigFactory.parseURL(url))
    }
}
package com.gitlab.ykrasik.gamedex.common.util

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/**
 * User: ykrasik
 * Date: 01/10/2016
 * Time: 21:56
 */
val defaultConfig: Config = run {
    val configurationFiles = ClassPathScanner.scanPackage("") {
        it.endsWith(".conf") && it != "application.conf" && it != "reference.conf"
    }

    // Use the default config as a baseline and apply 'withFallback' on it for every custom .conf file encountered.
    configurationFiles.fold(ConfigFactory.load()) { current, url ->
        current.withFallback(ConfigFactory.parseURL(url))
    }
}

fun <K, V> Config.getObjectMap(path: String, keyParser: (String) -> K, valueParser: (Any) -> V): Map<K, V> {
    return getObject(path).entries.associateBy(
        keySelector = { keyParser(it.key) },
        valueTransform = { valueParser(it.value.unwrapped()) }
    )
}
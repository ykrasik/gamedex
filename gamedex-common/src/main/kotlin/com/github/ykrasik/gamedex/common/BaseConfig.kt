package com.github.ykrasik.gamedex.common

import com.typesafe.config.Config

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:02
 */
// FIXME: I really don't like this, find a better way - databind.
abstract class BaseConfig(config: Config, scope: String) {
    protected val config: Config = config.getConfig(scope)
}
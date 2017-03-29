package com.gitlab.ykrasik.gamedex.module

import com.gitlab.ykrasik.gamedex.core.UIGameSearchChooser
import com.gitlab.ykrasik.gamedex.provider.GameSearchChooser
import com.gitlab.ykrasik.gamedex.util.UserPreferences
import com.google.inject.AbstractModule

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 11:16
 */
class AppModule : AbstractModule() {
    override fun configure() {
        bind(UserPreferences::class.java).toInstance(UserPreferences())
        bind(GameSearchChooser::class.java).to(UIGameSearchChooser::class.java)
    }
}
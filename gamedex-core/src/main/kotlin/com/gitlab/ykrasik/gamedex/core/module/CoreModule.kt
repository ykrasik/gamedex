package com.gitlab.ykrasik.gamedex.core.module

import com.gitlab.ykrasik.gamedex.core.UserPreferences
import com.gitlab.ykrasik.gamedex.core.UserPreferencesService
import com.google.inject.AbstractModule

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 11:16
 */
class CoreModule : AbstractModule() {
    override fun configure() {
        bind(UserPreferences::class.java).toInstance(UserPreferencesService.preferences())
    }
}
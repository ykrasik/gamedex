package com.gitlab.ykrasik.gamedex.core.module

import com.gitlab.ykrasik.gamedex.core.LibraryService
import com.gitlab.ykrasik.gamedex.core.LibraryServiceImpl
import com.gitlab.ykrasik.gamedex.core.UserPreferencesService
import com.gitlab.ykrasik.gamedex.core.UserPreferencesServiceImpl
import com.google.inject.AbstractModule

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 11:16
 */
class CoreModule : AbstractModule() {
    override fun configure() {
        bind(LibraryService::class.java).to(LibraryServiceImpl::class.java)
        bind(UserPreferencesService::class.java).to(UserPreferencesServiceImpl::class.java)
    }
}
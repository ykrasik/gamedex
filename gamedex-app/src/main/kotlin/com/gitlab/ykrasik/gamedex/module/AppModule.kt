package com.gitlab.ykrasik.gamedex.module

import com.gitlab.ykrasik.gamedex.core.*
import com.gitlab.ykrasik.gamedex.ui.model.GameRepository
import com.gitlab.ykrasik.gamedex.ui.model.LibraryRepository
import com.gitlab.ykrasik.gamedex.util.UserPreferences
import com.google.inject.AbstractModule

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 11:16
 */
object AppModule : AbstractModule() {
    override fun configure() {
        bind(UserPreferences::class.java).toInstance(UserPreferences())
        bind(DataProviderService::class.java).to(DataProviderServiceImpl::class.java)
        bind(GameSearchChooser::class.java).to(UIGameSearchChooser::class.java)

        // Instruct Guice to eagerly create these classes
        // (during preloading, to avoid the JavaFx thread from lazily creating them on first access)
        bind(GameRepository::class.java)
        bind(LibraryRepository::class.java)
        bind(LibraryScanner::class.java)
        bind(ImageLoader::class.java)
    }
}
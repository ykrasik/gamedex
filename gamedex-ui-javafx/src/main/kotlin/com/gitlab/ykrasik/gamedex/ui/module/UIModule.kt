package com.gitlab.ykrasik.gamedex.ui.module

import com.gitlab.ykrasik.gamedex.core.ui.ExcludedPathUIManager
import com.gitlab.ykrasik.gamedex.core.ui.GameUIManager
import com.gitlab.ykrasik.gamedex.core.ui.LibraryUIManager
import com.gitlab.ykrasik.gamedex.ui.controller.ExcludedPathController
import com.gitlab.ykrasik.gamedex.ui.controller.GameController
import com.gitlab.ykrasik.gamedex.ui.controller.LibraryController
import com.google.inject.AbstractModule

/**
 * User: ykrasik
 * Date: 15/10/2016
 * Time: 15:50
 */
class UIModule : AbstractModule() {
    override fun configure() {
        bind(GameUIManager::class.java).to(GameController::class.java)
        bind(LibraryUIManager::class.java).to(LibraryController::class.java)
        bind(ExcludedPathUIManager::class.java).to(ExcludedPathController::class.java)
    }
}
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

package com.gitlab.ykrasik.gamedex.javafx.module

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.image.ImageFactory
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.app.javafx.JavaFxViewManager
import com.gitlab.ykrasik.gamedex.app.javafx.image.JavaFxImageFactory
import com.gitlab.ykrasik.gamedex.core.provider.SearchChooser
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfig
import com.gitlab.ykrasik.gamedex.javafx.game.wall.GameWallUserConfig
import com.gitlab.ykrasik.gamedex.javafx.provider.JavaFxSearchChooser
import com.gitlab.ykrasik.gamedex.javafx.task.JavaFxTaskRunner
import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 11:16
 */
object JavaFxModule : AbstractModule() {
    override fun configure() {
        bind(ViewManager::class.java).to(JavaFxViewManager::class.java)
        bind(TaskRunner::class.java).to(JavaFxTaskRunner::class.java)
        bind(SearchChooser::class.java).to(JavaFxSearchChooser::class.java)
        bind(ImageFactory::class.java).toInstance(JavaFxImageFactory)

        with(Multibinder.newSetBinder(binder(), UserConfig::class.java)) {
            addBinding().to(GameWallUserConfig::class.java)
        }
    }
}
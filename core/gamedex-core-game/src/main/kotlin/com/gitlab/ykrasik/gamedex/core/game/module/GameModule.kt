/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.game.module

import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.game.GameServiceImpl
import com.gitlab.ykrasik.gamedex.core.game.presenter.*
import com.gitlab.ykrasik.gamedex.core.module.InternalCoreModule

/**
 * User: ykrasik
 * Date: 03/10/2018
 * Time: 09:47
 */
object GameModule : InternalCoreModule() {
    override fun configure() {
        bind(GameService::class.java).to(GameServiceImpl::class.java)

        bindPresenter(GamesPresenter::class)
        bindPresenter(PlatformPresenter::class)
        bindPresenter(GameDisplayTypePresenter::class)
        bindPresenter(CurrentPlatformFilterPresenter::class)
        bindPresenter(GameSortPresenter::class)

        bindPresenter(DeleteGamePresenter::class)
        bindPresenter(ShowDeleteGamePresenter::class)

        bindPresenter(GameDetailsPresenter::class)
        bindPresenter(ShowGameDetailsViewPresenter::class)

        bindPresenter(EditGamePresenter::class)
        bindPresenter(ShowEditGamePresenter::class)

        bindPresenter(RawGameDataPresenter::class)
        bindPresenter(DisplayRawGameDataPresenter::class)

        bindPresenter(ShowTagGamePresenter::class)
        bindPresenter(TagGamePresenter::class)

        bindPresenter(SetMainExecutableFilePresenter::class)
    }
}
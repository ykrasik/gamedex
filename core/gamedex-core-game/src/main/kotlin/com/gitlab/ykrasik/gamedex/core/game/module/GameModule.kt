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

package com.gitlab.ykrasik.gamedex.core.game.module

import com.gitlab.ykrasik.gamedex.core.game.GameConfig
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.game.GameServiceImpl
import com.gitlab.ykrasik.gamedex.core.game.presenter.GamesPresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.SearchGamesPresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.SelectPlatformPresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.SortGamesPresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.delete.DeleteGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.delete.ShowDeleteGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.details.GameDetailsPresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.details.ShowGameDetailsPreseneter
import com.gitlab.ykrasik.gamedex.core.game.presenter.discover.DiscoverGameChooseResultsPresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.discover.DiscoverGamesWithoutProvidersPresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.discover.DiscoverNewGamesPresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.discover.RediscoverGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.download.*
import com.gitlab.ykrasik.gamedex.core.game.presenter.edit.EditGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.edit.ShowEditGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.rename.RenameMoveGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.rename.ShowRenameMoveGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.tag.ShowTagGamePresenter
import com.gitlab.ykrasik.gamedex.core.game.presenter.tag.TagGamePresenter
import com.gitlab.ykrasik.gamedex.core.module.InternalCoreModule
import com.google.inject.Provides
import com.typesafe.config.Config
import io.github.config4k.extract
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 03/10/2018
 * Time: 09:47
 */
object GameModule : InternalCoreModule() {
    override fun configure() {
        bind(GameService::class.java).to(GameServiceImpl::class.java)

        bindPresenter(GamesPresenter::class)
        bindPresenter(SearchGamesPresenter::class)
        bindPresenter(SelectPlatformPresenter::class)
        bindPresenter(SortGamesPresenter::class)

        bindPresenter(DeleteGamePresenter::class)
        bindPresenter(ShowDeleteGamePresenter::class)

        bindPresenter(GameDetailsPresenter::class)
        bindPresenter(ShowGameDetailsPreseneter::class)

        bindPresenter(DiscoverGameChooseResultsPresenter::class)
        bindPresenter(DiscoverGamesWithoutProvidersPresenter::class)
        bindPresenter(DiscoverNewGamesPresenter::class)
        bindPresenter(RediscoverGamePresenter::class)

        bindPresenter(GameRedownloadCreatedBeforePeriodPresenter::class)
        bindPresenter(GameRedownloadUpdatedAfterPeriodPresenter::class)
        bindPresenter(RedownloadGamePresenter::class)
        bindPresenter(RedownloadGamesCreatedBeforePresenter::class)
        bindPresenter(RedownloadGamesUpdatedAfterPresenter::class)

        bindPresenter(EditGamePresenter::class)
        bindPresenter(ShowEditGamePresenter::class)

        bindPresenter(RenameMoveGamePresenter::class)
        bindPresenter(ShowRenameMoveGamePresenter::class)

        bindPresenter(ShowTagGamePresenter::class)
        bindPresenter(TagGamePresenter::class)
    }

    @Provides
    @Singleton
    fun gameConfig(config: Config): GameConfig = config.extract("gameDex.game")
}
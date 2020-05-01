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

package com.gitlab.ykrasik.gamedex.core.provider.module

import com.gitlab.ykrasik.gamedex.core.module.InternalCoreModule
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderServiceImpl
import com.gitlab.ykrasik.gamedex.core.provider.SyncGameService
import com.gitlab.ykrasik.gamedex.core.provider.SyncGameServiceImpl
import com.gitlab.ykrasik.gamedex.core.provider.presenter.*

/**
 * User: ykrasik
 * Date: 15/10/2018
 * Time: 16:46
 */
object ProviderModule : InternalCoreModule() {
    override fun configure() {
        bind(GameProviderService::class.java).to(GameProviderServiceImpl::class.java)
        bind(SyncGameService::class.java).to(SyncGameServiceImpl::class.java)

        bindPresenter(SyncLibrariesPresenter::class)
        bindPresenter(SyncGamesPresenter::class)
        bindPresenter(SyncGamePresenter::class)
        bindPresenter(ProviderSearchPresenter::class)

        bindPresenter(ShowBulkUpdateGamesPresenter::class)
        bindPresenter(UpdateGamePresenter::class)
        bindPresenter(BulkUpdateGamesPresenter::class)

        bindPresenter(ShowSyncGamesWithMissingProviders::class)
        bindPresenter(SyncGamesWithMissingProvidersPresenter::class)

        bind(ShowSyncGamesPresenter::class.java)
    }
}
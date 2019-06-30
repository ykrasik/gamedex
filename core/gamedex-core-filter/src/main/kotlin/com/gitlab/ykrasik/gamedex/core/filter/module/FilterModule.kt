/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.filter.module

import com.gitlab.ykrasik.gamedex.core.filter.*
import com.gitlab.ykrasik.gamedex.core.filter.presenter.*
import com.gitlab.ykrasik.gamedex.core.module.InternalCoreModule
import com.gitlab.ykrasik.gamedex.core.storage.StringIdJsonStorageFactory
import com.google.inject.Provides
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 18/09/2018
 * Time: 22:29
 */
object FilterModule : InternalCoreModule() {
    override fun configure() {
        bind(FilterService::class.java).to(FilterServiceImpl::class.java)

        bindPresenter(DeleteFilterPresenter::class)
        bindPresenter(EditFilterPresenter::class)
        bindPresenter(FilterPresenter::class)
        bindPresenter(FiltersPresenter::class)
        bindPresenter(ShowAddOrEditFilterPresenter::class)
        bindPresenter(ShowDeleteFilterPresenter::class)
    }

    @Provides
    @Singleton
    @UserFilters
    fun userFilterRepository() = FilterRepository("user", StringIdJsonStorageFactory("data/filters/user"))

    @Provides
    @Singleton
    @SystemFilters
    fun systemFilterRepository() = FilterRepository("system", StringIdJsonStorageFactory("data/filters/system"))
}
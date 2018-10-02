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

package com.gitlab.ykrasik.gamedex.core.library.module

import com.gitlab.ykrasik.gamedex.app.api.library.*
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.library.LibraryServiceImpl
import com.gitlab.ykrasik.gamedex.core.library.presenter.*
import com.gitlab.ykrasik.gamedex.core.module.InternalCoreModule

/**
 * User: ykrasik
 * Date: 02/10/2018
 * Time: 09:03
 */
object LibraryModule : InternalCoreModule() {
    override fun configure() {
        bind(LibraryService::class.java).to(LibraryServiceImpl::class.java)
        bindPresenter<DeleteLibraryPresenter, DeleteLibraryView>()
        bindPresenter<EditLibraryPresenter, EditLibraryView>()
        bindPresenter<LibrariesPresenter, ViewWithLibraries>()
        bindPresenter<ShowAddLibraryPresenter, ViewCanAddLibrary>()
        bindPresenter<ShowDeleteLibraryPresenter, ViewCanDeleteLibrary>()
        bindPresenter<ShowEditLibraryPresenter, ViewCanEditLibrary>()
    }
}
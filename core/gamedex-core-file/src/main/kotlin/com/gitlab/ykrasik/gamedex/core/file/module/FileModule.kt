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

package com.gitlab.ykrasik.gamedex.core.file.module

import com.gitlab.ykrasik.gamedex.FileStructure
import com.gitlab.ykrasik.gamedex.GameId
import com.gitlab.ykrasik.gamedex.core.file.FileStructureStorage
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.file.FileSystemServiceImpl
import com.gitlab.ykrasik.gamedex.core.file.presenter.BrowsePathPresenter
import com.gitlab.ykrasik.gamedex.core.module.InternalCoreModule
import com.gitlab.ykrasik.gamedex.core.storage.FileStorage
import com.gitlab.ykrasik.gamedex.core.storage.Storage
import com.google.inject.Provides
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 12/10/2018
 * Time: 10:18
 */
object FileModule : InternalCoreModule() {
    override fun configure() {
        bind(FileSystemService::class.java).to(FileSystemServiceImpl::class.java)

        bindPresenter(BrowsePathPresenter::class)
    }

    @Provides
    @Singleton
    @FileStructureStorage
    fun fileStructureStorage(): Storage<GameId, FileStructure> =
        FileStorage.json<FileStructure>("cache/file_structure").intId()
}
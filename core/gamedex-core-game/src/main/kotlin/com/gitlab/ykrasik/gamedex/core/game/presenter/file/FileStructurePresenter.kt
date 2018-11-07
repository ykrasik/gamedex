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

package com.gitlab.ykrasik.gamedex.core.game.presenter.file

import com.gitlab.ykrasik.gamedex.app.api.game.ViewWithGameFileStructure
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 11/10/2018
 * Time: 09:27
 */
@Singleton
class FileStructurePresenter @Inject constructor(private val fileSystemService: FileSystemService) : Presenter<ViewWithGameFileStructure> {
    override fun present(view: ViewWithGameFileStructure) = object : ViewSession() {
        init {
            view.gameChanges.forEach {
                view.fileStructure = fileSystemService.structure(it)
            }
        }
    }
}
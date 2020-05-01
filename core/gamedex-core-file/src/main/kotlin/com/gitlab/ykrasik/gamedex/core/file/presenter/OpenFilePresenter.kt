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

package com.gitlab.ykrasik.gamedex.core.file.presenter

import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanOpenFile
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 12/10/2018
 * Time: 09:30
 */
@Singleton
class OpenFilePresenter @Inject constructor() : Presenter<ViewCanOpenFile> {
    override fun present(view: ViewCanOpenFile) = object : ViewSession() {
        init {
            view.openFileActions.forEach { file ->
                try {
                    withContext(Dispatchers.IO) {
                        // TODO: This is actually more like view-specific logic.
                        Desktop.getDesktop().open(file)
                    }
                } catch (e: Exception) {
                    view.onError(message = file.toString(), title = "File doesn't exist", e = e)
                }
            }
        }
    }
}
/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.web.presenter

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.web.ViewCanBrowseUrl
import com.gitlab.ykrasik.gamedex.core.settings.GeneralSettingsRepository
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewService
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import com.gitlab.ykrasik.gamedex.util.toUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 12/10/2018
 * Time: 09:48
 */
@Singleton
class BrowseUrlPresenter @Inject constructor(
    private val viewService: ViewService,
    private val settingsRepo: GeneralSettingsRepository,
) : Presenter<ViewCanBrowseUrl> {
    override fun present(view: ViewCanBrowseUrl) = object : ViewSession() {
        init {
            view::browseUrlActions.forEach { url ->
                if (settingsRepo.useInternalBrowser.value) {
                    viewService.showAndHide(ViewManager::showBrowserView, ViewManager::hide, url)
                } else {
                    launch(Dispatchers.IO) {
                        val customBrowserCommand = settingsRepo.customBrowserCommand.value
                        if (customBrowserCommand.isBlank()) {
                            Desktop.getDesktop().browse(url.toUrl().toURI())
                        } else {
                            Runtime.getRuntime().exec((customBrowserCommand.split(Pattern.compile("\\s+")) + url).toTypedArray())
                        }
                    }
                }
            }
        }
    }
}

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

package com.gitlab.ykrasik.gamedex.core.web.presenter

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.web.BrowserView
import com.gitlab.ykrasik.gamedex.app.api.web.ViewCanBrowseUrl
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.hideViewRequests
import com.gitlab.ykrasik.gamedex.core.settings.GeneralSettingsRepository
import com.gitlab.ykrasik.gamedex.core.util.flowScope
import com.gitlab.ykrasik.gamedex.util.toUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 12/10/2018
 * Time: 09:48
 */
@Singleton
class BrowseUrlPresenter @Inject constructor(
    private val settingsRepo: GeneralSettingsRepository,
    private val viewManager: ViewManager,
    eventBus: EventBus
) : Presenter<ViewCanBrowseUrl> {
    init {
        flowScope(Dispatchers.Main.immediate) {
            eventBus.hideViewRequests<BrowserView>().forEach(debugName = "hideBrowserView") { viewManager.hide(it) }
        }
    }

    override fun present(view: ViewCanBrowseUrl) = object : ViewSession() {
        init {
            view::browseUrlActions.forEach {
                if (settingsRepo.useInternalBrowser.value) {
                    viewManager.showBrowserView(it)
                } else {
                    // FIXME: This logic belongs to the view
                    launch(Dispatchers.IO) {
                        Desktop.getDesktop().browse(it.toUrl().toURI())
                    }
                }
            }
        }
    }
}
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

package com.gitlab.ykrasik.gamedex.app.api.game.discover

import com.gitlab.ykrasik.gamedex.app.api.Presenter
import com.gitlab.ykrasik.gamedex.app.api.ViewCanRunTask

/**
 * User: ykrasik
 * Date: 29/04/2018
 * Time: 13:31
 */
// FIXME: Doesn't look like this is needed, this feels like a part of the GameScreenPresenter.
interface GameDiscoveryView : ViewCanRunTask<GameDiscoveryView.Event> {
    sealed class Event {
        object SearchNewGamesClicked : Event()
        object SearchGamesWithoutProvidersClicked : Event()
    }
}

interface GameDiscoveryPresenter : Presenter<GameDiscoveryView>
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

package com.gitlab.ykrasik.gamedex.app.api.game.tag

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.PresenterFactory

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 18:10
 */
interface TagGameViewPresenter {
    fun onShown(game: Game)

    fun onAccept()
    fun onCancel()

    fun onToggleAllChanged(toggleAll: Boolean)
    fun onTagToggleChanged(tag: String, toggle: Boolean)

    fun onNewTagNameChanged(name: String)
    fun onAddNewTag()
}

interface TagGameView {
    var game: Game

    val tags: MutableList<String>
    val checkedTags: MutableSet<String>

    var toggleAll: Boolean

    var newTagName: String
    var nameValidationError: String?

    fun close(choice: TagGameChoice)
}

interface TagGameViewPresenterFactory : PresenterFactory<TagGameView, TagGameViewPresenter>
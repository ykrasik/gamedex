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

package com.gitlab.ykrasik.gamedex.core.game.tag

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.RawGame
import com.gitlab.ykrasik.gamedex.UserData
import com.gitlab.ykrasik.gamedex.app.api.game.tag.TagGamePresenter
import com.gitlab.ykrasik.gamedex.app.api.game.tag.TagGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.tag.TagGameView
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.launchOnUi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 18:25
 */
@Singleton
class TagGamePresenterFactoryImpl @Inject constructor(
    private val gameService: GameService,
    private val taskRunner: TaskRunner
) : TagGamePresenterFactory {
    override fun present(view: TagGameView): TagGamePresenter = object : TagGamePresenter {
        private var ignoreNextUntoggleAll = false

        override fun onShown(game: Game) {
            view.game = game
            view.tags.clear()
            view.tags.addAll(gameService.tags)
            view.checkedTags.clear()
            view.checkedTags.addAll(game.tags)
            view.toggleAll = view.tags.toSet() == game.tags.toSet()
            view.nameValidationError = null
        }

        override fun onToggleAllChanged(toggleAll: Boolean) {
            if (toggleAll) {
                view.checkedTags.addAll(view.tags)
            } else {
                if (!ignoreNextUntoggleAll) {
                    view.checkedTags.clear()
                }
                ignoreNextUntoggleAll = false
            }
        }

        override fun onTagToggleChanged(tag: String, toggle: Boolean) {
            if (toggle) {
                ignoreNextUntoggleAll = false
                view.checkedTags += tag
                if (view.checkedTags == view.tags.toSet()) {
                    view.toggleAll = true
                }
            } else {
                ignoreNextUntoggleAll = true
                view.checkedTags -= tag
                view.toggleAll = false
            }
        }

        override fun onNewTagNameChanged(name: String) {
            view.nameValidationError = when {
                name.isEmpty() -> ""
                view.tags.contains(name) -> "Tag already exists!"
                else -> null
            }
        }

        override fun onAddNewTag() {
            view.tags += view.newTagName
            view.checkedTags += view.newTagName
            view.newTagName = ""
        }

        override fun onAccept() = launchOnUi {
            val newRawGame = view.game.rawGame.withTags(view.checkedTags)
            if (newRawGame.userData != view.game.rawGame.userData) {
                taskRunner.runTask(gameService.replace(view.game, newRawGame))
            }
            view.closeView()
        }

        private fun RawGame.withTags(tags: Collection<String>): RawGame {
            // If new tags are empty and userData is null, or userData has empty tags -> nothing to do
            // If new tags are not empty and userData is not null, but has the same tags -> nothing to do
            val newTags = tags.toList().sorted()
            if (newTags == userData?.tags ?: emptyList<String>()) return this

            val userData = this.userData ?: UserData()
            return copy(userData = userData.copy(tags = newTags))
        }

        override fun onCancel() {
            view.closeView()
        }

    }
}
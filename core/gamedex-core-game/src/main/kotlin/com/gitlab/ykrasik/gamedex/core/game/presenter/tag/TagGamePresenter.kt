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

package com.gitlab.ykrasik.gamedex.core.game.presenter.tag

import com.gitlab.ykrasik.gamedex.RawGame
import com.gitlab.ykrasik.gamedex.UserData
import com.gitlab.ykrasik.gamedex.app.api.game.TagGameView
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.setAll
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 18:25
 */
@Singleton
class TagGamePresenter @Inject constructor(
    private val gameService: GameService,
    private val commonData: CommonData,
    private val taskService: TaskService,
    private val eventBus: EventBus
) : Presenter<TagGameView> {
    override fun present(view: TagGameView) = object : ViewSession() {
        init {
            view.toggleAll.forEach { onToggleAllChanged(it) }
            view.checkTagChanges.forEach { (tag, checked) -> onCheckTagChanged(tag, checked) }
            view.newTagName.forEach { onNewTagNameChanged(it) }

            view.addNewTagActions.forEach { onAddNewTag() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override fun onShow() {
            val game = view.game
            view.tags.setAll(commonData.tags)
            view.checkedTags.setAll(game.tags)
            view.toggleAll *= view.tags.toSet() == game.tags.toSet()
            view.newTagName *= ""
            validateNewTag("")
            setCanAccept()
        }

        private fun onToggleAllChanged(toggleAll: Boolean) {
            if (toggleAll) {
                view.checkedTags.addAll(view.tags)
            } else {
                view.checkedTags.clear()
            }
            setCanAccept()
        }

        private fun onCheckTagChanged(tag: String, checked: Boolean) {
            if (checked) {
                view.checkedTags += tag
                if (view.checkedTags == view.tags.toSet()) {
                    view.toggleAll *= true
                }
            } else {
                view.checkedTags -= tag
                view.toggleAll *= false
            }
            setCanAccept()
        }

        private fun setCanAccept() {
            view.canAccept *= IsValid {
                check(view.checkedTags != view.game.tags.toSet()) { "Nothing changed!" }
            }
        }

        private fun onNewTagNameChanged(name: String) {
            validateNewTag(name)
        }

        private fun validateNewTag(name: String) {
            view.newTagNameIsValid *= IsValid {
                if (name.isEmpty()) error("Empty Name!")
                if (view.tags.contains(name)) error("Tag already exists!")
            }
        }

        private fun onAddNewTag() {
            view.tags += view.newTagName.value
            view.checkedTags += view.newTagName.value
            view.newTagName *= ""
            setCanAccept()
        }

        private suspend fun onAccept() {
            val newRawGame = view.game.rawGame.withTags(view.checkedTags)
            if (newRawGame.userData != view.game.rawGame.userData) {
                taskService.execute(gameService.replace(view.game, newRawGame))
            }

            finished()
        }

        private fun RawGame.withTags(tags: Collection<String>): RawGame {
            // If new tags are empty and userData is null, or userData has empty tags -> nothing to do
            // If new tags are not empty and userData is not null, but has the same tags -> nothing to do
            val newTags = tags.toList().sorted()
            if (newTags == userData?.tags ?: emptyList<String>()) return this

            val userData = this.userData ?: UserData()
            return copy(userData = userData.copy(tags = newTags))
        }

        private fun onCancel() {
            finished()
        }

        private fun finished() = eventBus.viewFinished(view)
    }
}
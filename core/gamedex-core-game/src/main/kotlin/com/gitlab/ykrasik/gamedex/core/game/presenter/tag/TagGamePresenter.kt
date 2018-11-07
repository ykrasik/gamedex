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
import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.game.TagGameView
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
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
    private val viewManager: ViewManager
) : Presenter<TagGameView> {
    override fun present(view: TagGameView) = object : Presentation() {
        private var ignoreNextUncheckAll = false

        init {
            view.checkAllChanges.forEach { onCheckAllChanged(it) }
            view.checkTagChanges.forEach { (tag, checked) -> onCheckTagChanged(tag, checked) }
            view.newTagNameChanges.forEach { onNewTagNameChanged(it) }

            view.addNewTagActions.forEach { onAddNewTag() }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override fun onShow() {
            val game = view.game
            view.tags.setAll(commonData.tags)
            view.checkedTags.setAll(game.tags)
            view.toggleAll = view.tags.toSet() == game.tags.toSet()
            view.nameValidationError = null
            ignoreNextUncheckAll = false
        }

        private fun onCheckAllChanged(checkAll: Boolean) {
            if (checkAll) {
                view.checkedTags.addAll(view.tags)
            } else {
                if (!ignoreNextUncheckAll) {
                    view.checkedTags.clear()
                }
                ignoreNextUncheckAll = false
            }
        }

        private fun onCheckTagChanged(tag: String, checked: Boolean) {
            if (checked) {
                ignoreNextUncheckAll = false
                view.checkedTags += tag
                if (view.checkedTags == view.tags.toSet()) {
                    view.toggleAll = true
                }
            } else {
                ignoreNextUncheckAll = true
                view.checkedTags -= tag
                view.toggleAll = false
            }
        }

        private fun onNewTagNameChanged(name: String) {
            view.nameValidationError = when {
                name.isEmpty() -> ""
                view.tags.contains(name) -> "Tag already exists!"
                else -> null
            }
        }

        private fun onAddNewTag() {
            view.tags += view.newTagName
            view.checkedTags += view.newTagName
            view.newTagName = ""
        }

        private suspend fun onAccept() {
            val newRawGame = view.game.rawGame.withTags(view.checkedTags)
            if (newRawGame.userData != view.game.rawGame.userData) {
                taskService.execute(gameService.replace(view.game, newRawGame))
            }

            close()
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
            close()
        }

        private fun close() = viewManager.closeTagGameView(view)
    }
}
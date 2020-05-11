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

package com.gitlab.ykrasik.gamedex.core.game.presenter

import com.gitlab.ykrasik.gamedex.app.api.game.TagGameView
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.setAll
import kotlinx.coroutines.flow.map
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
        private val game by view.game

        init {
            view.tags *= commonData.tags

            isShowing.forEach {
                if (it) {
                    view.checkedTags.setAll(game.tags)
                    view.toggleAll *= view.tags.toSet() == game.tags.toSet()
                    view.newTagName *= ""
                }
            }
            view.newTagNameIsValid *= view.newTagName.allValues().map { name ->
                IsValid {
                    if (name.isEmpty()) error("Empty Name!")
                    if (view.tags.contains(name)) error("Tag already exists!")
                }
            } withDebugName "newTagNameIsValid"
            view.toggleAll.onlyChangesFromView().forEach {
                if (it) {
                    view.checkedTags.addAll(view.tags)
                } else {
                    view.checkedTags.clear()
                }
            }
            view.checkTagChanges.forEach(debugName = "onCheckTagChanged") { (tag, checked) ->
                if (checked) {
                    view.checkedTags += tag
                    if (view.checkedTags == view.tags.toSet()) {
                        view.toggleAll *= true
                    }
                } else {
                    view.checkedTags -= tag
                    view.toggleAll *= false
                }
            }
            view.addNewTagActions.forEach(debugName = "onAddNewTag") {
                view.tags += view.newTagName.v
                view.checkedTags += view.newTagName.v
                view.newTagName *= ""
            }

            view.acceptActions.forEach(debugName = "onAccept") { onAccept() }
            view.cancelActions.forEach(debugName = "onCancel") { onCancel() }
        }

        private suspend fun onAccept() {
            view.canAccept.assert()

            val checkedTags = view.checkedTags.toList().sorted()
            if (checkedTags != game.tags.sorted()) {
                val newUserData = game.userData.copy(tags = checkedTags)
                val newRawGame = game.rawGame.copy(userData = newUserData)
                taskService.execute(gameService.replace(game, newRawGame))
            }

            hideView()
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}
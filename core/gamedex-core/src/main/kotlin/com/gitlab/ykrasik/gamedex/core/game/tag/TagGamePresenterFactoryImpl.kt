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
import com.gitlab.ykrasik.gamedex.app.api.game.tag.TagGameChoice
import com.gitlab.ykrasik.gamedex.app.api.game.tag.TagGamePresenter
import com.gitlab.ykrasik.gamedex.app.api.game.tag.TagGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.tag.ViewCanTagGame
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.runOnUi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/05/2018
 * Time: 22:03
 */
@Singleton
class TagGamePresenterFactoryImpl @Inject constructor(
    private val taskRunner: TaskRunner,
    private val gameService: GameService
) : TagGamePresenterFactory {
    override fun present(view: ViewCanTagGame): TagGamePresenter = object : TagGamePresenter {
        override suspend fun tagGame(game: Game): Game? = runOnUi {
            val choice = view.showTagGameView(game)
            val tags = when (choice) {
                is TagGameChoice.Select -> choice.tags
                is TagGameChoice.Cancel -> return@runOnUi null
            }

            val newRawGame = game.rawGame.withTags(tags)
            if (newRawGame.userData != game.rawGame.userData) {
                taskRunner.runTask(gameService.replace(game, newRawGame))
            } else {
                null
            }
        }
    }

    private fun RawGame.withTags(tags: List<String>): RawGame {
        // If new tags are empty and userData is null, or userData has empty tags -> nothing to do
        // If new tags are not empty and userData is not null, but has the same tags -> nothing to do
        if (tags == userData?.tags ?: emptyList<String>()) return this

        val userData = this.userData ?: UserData()
        return copy(userData = userData.copy(tags = tags))
    }
}
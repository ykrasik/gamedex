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

package com.gitlab.ykrasik.gamedex.core.game.common

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.app.api.game.common.EditGameDetailsChoice
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/04/2018
 * Time: 20:56
 */
// TODO: Delete this class.
@Singleton
class CommonGamePresenterOps @Inject constructor(
    private val taskRunner: TaskRunner,
    private val gameService: GameService
) {
    suspend fun editDetails(game: Game, showEditView: (Game) -> EditGameDetailsChoice): Game? {
        val choice = showEditView(game)
        val overrides = when (choice) {
            is EditGameDetailsChoice.Override -> choice.overrides
            EditGameDetailsChoice.Clear -> emptyMap()
            EditGameDetailsChoice.Cancel -> return null
        }

        val newRawGame = game.rawGame.withDataOverrides(overrides)
        return if (newRawGame.userData != game.rawGame.userData) {
            taskRunner.runTask(gameService.replace(game, newRawGame))
        } else {
            null
        }
    }

    private fun RawGame.withDataOverrides(overrides: Map<GameDataType, GameDataOverride>): RawGame {
        // If new overrides are empty and userData is null, or userData has empty overrides -> nothing to do
        // If new overrides are not empty and userData is not null, but has the same overrides -> nothing to do
        if (overrides == userData?.overrides ?: emptyMap<GameDataType, GameDataOverride>()) return this

        val userData = this.userData ?: UserData()
        return copy(userData = userData.copy(overrides = overrides))
    }
}

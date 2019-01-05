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

package com.gitlab.ykrasik.gamedex.app.api.provider

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.LibraryPath
import com.gitlab.ykrasik.gamedex.app.api.State
import com.gitlab.ykrasik.gamedex.app.api.UserMutableState
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.Modifier
import com.gitlab.ykrasik.gamedex.util.MultiMap
import com.gitlab.ykrasik.gamedex.util.modifyLast
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * User: ykrasik
 * Date: 12/10/2018
 * Time: 20:22
 */
interface ProviderSearchView {
    val state: State<GameSearchState>

    val query: UserMutableState<String>

    val searchResults: MutableList<ProviderSearchResult>
    val selectedSearchResult: UserMutableState<ProviderSearchResult?>

    val canChangeState: State<IsValid>
    val canSearchCurrentQuery: State<IsValid>
    val canAcceptSearchResult: State<IsValid>
    val canExcludeSearchResult: State<IsValid>

    val canToggleFilterPreviouslyDiscardedResults: State<IsValid>
    val isFilterPreviouslyDiscardedResults: UserMutableState<Boolean>

    val isAllowSmartChooseResults: State<Boolean>
    val canSmartChooseResult: State<Boolean>

    val choiceActions: ReceiveChannel<ProviderSearchChoice>
    val changeProviderActions: ReceiveChannel<ProviderId>
}

data class GameSearchState(
    val libraryPath: LibraryPath,
    val providerOrder: List<ProviderId>,
    val currentProvider: ProviderId?,
    val history: MultiMap<ProviderId, GameSearch>,
    val status: GameSearchStatus,
    val game: Game?
) {
    val isFinished: Boolean get() = status != GameSearchStatus.Running
    fun historyFor(providerId: ProviderId): List<GameSearch> = history.getOrDefault(providerId, emptyList())
    fun lastSearchFor(providerId: ProviderId): GameSearch? = historyFor(providerId).lastOrNull()
    val currentSearch: GameSearch? get() = lastSearchFor(currentProvider!!)

    inline fun <reified T : ProviderSearchChoice> choicesOfType(): Sequence<Pair<ProviderId, T>> =
        history.asSequence().mapNotNull { (providerId, results) -> (results.last().choice as? T)?.let { providerId to it } }

    val choices: Sequence<Pair<ProviderId, ProviderSearchChoice>> get() = choicesOfType()

    val progress: Double get() = choices.toList().size.toDouble() / providerOrder.size

    inline fun modifyHistory(providerId: ProviderId, f: Modifier<List<GameSearch>>): GameSearchState =
        copy(history = history + (providerId to f(historyFor(providerId))))

    inline fun modifyCurrentProviderHistory(f: Modifier<List<GameSearch>>): GameSearchState =
        modifyHistory(currentProvider!!, f)

    inline fun modifyCurrentSearch(f: Modifier<GameSearch>): GameSearchState =
        modifyCurrentProviderHistory { modifyLast(f) }

    companion object {
        val Null = GameSearchState(
            libraryPath = LibraryPath.Null,
            providerOrder = emptyList(),
            currentProvider = "",
            history = emptyMap(),
            status = GameSearchStatus.Cancelled,
            game = null
        )
    }
}

enum class GameSearchStatus {
    Running, Success, Cancelled
}

data class GameSearch(
    val provider: ProviderId,
    val query: String,
    val results: List<ProviderSearchResult>,
    val choice: ProviderSearchChoice?
)

sealed class ProviderSearchChoice {
    data class Accept(val result: ProviderSearchResult) : ProviderSearchChoice()
    data class NewSearch(val newQuery: String) : ProviderSearchChoice()
    object Skip : ProviderSearchChoice()
    object Exclude : ProviderSearchChoice()
    object Cancel : ProviderSearchChoice()

    val isResult: Boolean get() = this !is NewSearch && this !is Cancel
    val isNonExcludeResult: Boolean get() = this is Accept || this is Skip
}
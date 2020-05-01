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

package com.gitlab.ykrasik.gamedex.app.api.provider

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.LibraryPath
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.app.api.util.MultiReceiveChannel
import com.gitlab.ykrasik.gamedex.app.api.util.SettableList
import com.gitlab.ykrasik.gamedex.app.api.util.State
import com.gitlab.ykrasik.gamedex.app.api.util.UserMutableState
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.Modifier
import com.gitlab.ykrasik.gamedex.util.MultiMap
import com.gitlab.ykrasik.gamedex.util.modifyLast

/**
 * User: ykrasik
 * Date: 12/10/2018
 * Time: 20:22
 */
interface ProviderSearchView {
    val state: State<GameSearchState>

    val query: UserMutableState<String>

    val searchResults: SettableList<GameProvider.SearchResult>
    val selectedSearchResult: UserMutableState<GameProvider.SearchResult?>
    val fetchSearchResultActions: MultiReceiveChannel<GameProvider.SearchResult>

    val canChangeState: State<IsValid>
    val canSearchCurrentQuery: State<IsValid>
    val canAcceptSearchResult: State<IsValid>
    val canExcludeSearchResult: State<IsValid>

    val canToggleFilterPreviouslyDiscardedResults: State<IsValid>
    val isFilterPreviouslyDiscardedResults: UserMutableState<Boolean>

    val isAllowSmartChooseResults: State<Boolean>
    val canSmartChooseResult: State<Boolean>

    val choiceActions: MultiReceiveChannel<GameSearchState.ProviderSearch.Choice>
    val changeProviderActions: MultiReceiveChannel<ProviderId>

    val canShowMoreResults: State<IsValid>
    val showMoreResultsActions: MultiReceiveChannel<Unit>
}

data class GameSearchState(
    val index: Int,
    val libraryPath: LibraryPath,
    val providerOrder: List<ProviderId>,
    val currentProvider: ProviderId?,
    val history: MultiMap<ProviderId, ProviderSearch>,
    val status: Status,
    val existingGame: Game?
) {
    val isFinished: Boolean get() = status != Status.Running
    fun historyFor(providerId: ProviderId): List<ProviderSearch> = history.getOrDefault(providerId, emptyList())
    fun lastSearchFor(providerId: ProviderId): ProviderSearch? = historyFor(providerId).lastOrNull()
    val currentProviderSearch: ProviderSearch? get() = currentProvider?.let(::lastSearchFor)

    inline fun <reified T : ProviderSearch.Choice> choicesOfType(): Sequence<Pair<ProviderId, T>> =
        history.asSequence().mapNotNull { (providerId, results) -> (results.last().choice as? T)?.let { providerId to it } }

    val choices: Sequence<Pair<ProviderId, ProviderSearch.Choice>> get() = choicesOfType()

    val progress: Double get() = choices.toList().size.toDouble() / providerOrder.size

    inline fun modifyHistory(providerId: ProviderId, f: Modifier<List<ProviderSearch>>): GameSearchState =
        copy(history = history + (providerId to f(historyFor(providerId))))

    inline fun modifyCurrentProviderHistory(f: Modifier<List<ProviderSearch>>): GameSearchState =
        modifyHistory(currentProvider!!, f)

    inline fun modifyCurrentProviderSearch(f: Modifier<ProviderSearch>): GameSearchState =
        modifyCurrentProviderHistory { modifyLast(f) }

    companion object {
        val Null = GameSearchState(
            index = -1,
            libraryPath = LibraryPath.Null,
            providerOrder = emptyList(),
            currentProvider = "",
            history = emptyMap(),
            status = Status.Cancelled,
            existingGame = null
        )
    }

    enum class Status {
        Running, Success, Cancelled
    }

    data class ProviderSearch(
        val provider: ProviderId,
        val query: String,
        val offset: Int,
        val results: List<GameProvider.SearchResult>,
        val canShowMoreResults: Boolean,
        val choice: Choice?
    ) {
        sealed class Choice {
            data class Accept(val result: GameProvider.SearchResult) : ProviderSearch.Choice()
            data class NewSearch(val newQuery: String) : ProviderSearch.Choice()
            object Skip : ProviderSearch.Choice()
            object Exclude : ProviderSearch.Choice()
            object Cancel : ProviderSearch.Choice()

            // Should never be sent by the view, this is a synthetic choice used in syncing missing providers.
            data class Preset(val result: GameProvider.SearchResult, val data: ProviderData) : ProviderSearch.Choice()

            val isResult: Boolean get() = this !is NewSearch && this !is Cancel
            val isNonExcludeResult: Boolean get() = this is Accept || this is Preset || this is Skip
        }
    }
}
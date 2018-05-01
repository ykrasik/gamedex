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

package com.gitlab.ykrasik.gamedex.app.api

import com.gitlab.ykrasik.gamedex.app.api.game.common.DeleteGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.common.EditGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.common.TagGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.details.GameDetailsPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.discover.DiscoverGameChooseResultsPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.discover.DiscoverGamesWithoutProvidersPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.discover.DiscoverNewGamesPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.discover.RediscoverGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.download.GameDownloadStaleDurationPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.download.RedownloadAllStaleGamesPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.download.RedownloadGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.general.CleanupDbPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.general.ClearUserDataPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.general.ExportDatabasePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.general.ImportDatabasePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.library.*
import com.gitlab.ykrasik.gamedex.app.api.log.LogEntriesPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.log.LogLevelPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.log.LogTailPresenterFactory
import com.gitlab.ykrasik.gamedex.util.InitOnceGlobal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 21/04/2018
 * Time: 16:55
 */
interface PresenterFactory<in V, out P> {
    fun present(view: V): P
}

@Singleton
data class Presenters @Inject constructor(
    val libraries: LibrariesPresenterFactory,
    val addLibrary: AddLibraryPresenterFactory,
    val editLibrary: EditLibraryPresenterFactory,
    val deleteLibrary: DeleteLibraryPresenterFactory,
    val editLibraryView: EditLibraryViewPresenterFactory,

    val editGame: EditGamePresenterFactory,
    val deleteGame: DeleteGamePresenterFactory,
    val tagGame: TagGamePresenterFactory,
    val gameDetails: GameDetailsPresenterFactory,
    val discoverGameChooseResults: DiscoverGameChooseResultsPresenterFactory,
    val discoverGamesWithoutProviders: DiscoverGamesWithoutProvidersPresenterFactory,
    val discoverNewGames: DiscoverNewGamesPresenterFactory,
    val rediscoverGame: RediscoverGamePresenterFactory,
    val redownloadGame: RedownloadGamePresenterFactory,

    val gameDownloadStaleDuration: GameDownloadStaleDurationPresenterFactory,
    val redownloadAllStaleGames: RedownloadAllStaleGamesPresenterFactory,

    val logEntries: LogEntriesPresenterFactory,
    val logLevel: LogLevelPresenterFactory,
    val logTail: LogTailPresenterFactory,

    val exportDatabase: ExportDatabasePresenterFactory,
    val importDatabase: ImportDatabasePresenterFactory,
    val clearUserData: ClearUserDataPresenterFactory,
    val cleanupDb: CleanupDbPresenterFactory
)

var presenters: Presenters by InitOnceGlobal()
/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.maintenance.module

import com.gitlab.ykrasik.gamedex.core.maintenance.presenter.*
import com.gitlab.ykrasik.gamedex.core.module.InternalCoreModule

/**
 * User: ykrasik
 * Date: 12/10/2018
 * Time: 10:31
 */
object MaintenanceModule : InternalCoreModule() {
    override fun configure() {
        bindPresenter(ShowExportDatabasePresenter::class)
        bindPresenter(ExportDatabasePresenter::class)

        bindPresenter(ShowImportDatabasePresenter::class)
        bindPresenter(ImportDatabasePresenter::class)

        bindPresenter(ClearUserDataPresenter::class)

        bindPresenter(ShowCleanupDatabasePresenter::class)
        bindPresenter(CleanupDatabasePresenter::class)

        bindPresenter(ShowDuplicatesReportPresenter::class)
        bindPresenter(DuplicatesReportPresenter::class)

        bindPresenter(ShowFolderNameDiffReportPresenter::class)
        bindPresenter(FolderNameDiffReportPresenter::class)
    }
}
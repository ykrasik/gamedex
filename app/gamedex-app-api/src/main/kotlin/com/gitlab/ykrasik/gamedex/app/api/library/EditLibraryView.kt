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

package com.gitlab.ykrasik.gamedex.app.api.library

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryType
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.ConfirmationView
import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStateFlow
import com.gitlab.ykrasik.gamedex.util.IsValid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * User: ykrasik
 * Date: 21/04/2018
 * Time: 07:05
 */
interface EditLibraryView : ConfirmationView {
    val library: ViewMutableStateFlow<Library?>

    val name: ViewMutableStateFlow<String>
    val nameIsValid: MutableStateFlow<IsValid>

    val path: ViewMutableStateFlow<String>
    val pathIsValid: MutableStateFlow<IsValid>

    val type: ViewMutableStateFlow<LibraryType>
    val canChangeType: MutableStateFlow<IsValid>

    val platform: ViewMutableStateFlow<Platform?>
    val shouldShowPlatform: MutableStateFlow<IsValid>
    val canChangePlatform: MutableStateFlow<IsValid>

    val browseActions: Flow<Unit>
    fun browse(initialDirectory: File?): File?
}
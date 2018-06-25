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

package com.gitlab.ykrasik.gamedex.core.api.util

import com.gitlab.ykrasik.gamedex.util.InitOnceGlobal
import kotlinx.coroutines.experimental.CoroutineDispatcher

/**
 * User: ykrasik
 * Date: 24/03/2018
 * Time: 15:56
 */
// FIXME: Move to gamedex-app-api & rename file to UIThreadDispatcher
var uiThreadDispatcher: CoroutineDispatcher by InitOnceGlobal()
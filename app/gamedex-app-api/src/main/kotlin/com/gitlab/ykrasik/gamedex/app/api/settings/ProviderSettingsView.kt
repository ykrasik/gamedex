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

package com.gitlab.ykrasik.gamedex.app.api.settings

import com.gitlab.ykrasik.gamedex.app.api.State
import com.gitlab.ykrasik.gamedex.app.api.UserMutableState
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.util.IsValid
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * User: ykrasik
 * Date: 20/06/2018
 * Time: 09:44
 */
interface ProviderSettingsView {
    val provider: GameProvider

    val status: State<ProviderAccountStatus>
    val enabled: UserMutableState<Boolean>

    val currentAccount: UserMutableState<Map<String, String>>

    val gotoAccountUrlActions: ReceiveChannel<Unit>

    val canVerifyAccount: State<IsValid>
    val verifyAccountActions: ReceiveChannel<Unit>
}

enum class ProviderAccountStatus {
    Valid, Invalid, Empty, Unverified, NotRequired
}
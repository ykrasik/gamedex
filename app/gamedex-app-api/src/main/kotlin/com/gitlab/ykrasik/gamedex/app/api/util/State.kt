/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.api.util

/**
 * User: ykrasik
 * Date: 02/12/2018
 * Time: 16:44
 */

/**
 * State that can be changed by setting [value] from code.
 * The view holding this state is expected to react to such changes, in a manner appropriate to the view.
 */
interface State<T> {
    /**
     * Used to notify the view about value changes.
     */
    var value: T
}

/**
 * State that can both be changed from code by setting [value] (in which case the view should react to the change)
 * as well as be changed by the user, in which case the change is reported to the [changes] channel.
 * Such changes will trigger code listening to these changes to react.
 */
interface UserMutableState<T> : State<T> {
    /**
     * Used to notify the view about value changes. Must not trigger an event on [changes]!
     */
    override var value: T

    /**
     * Reports changes the user made to the value from the view.
     */
    val changes: MultiReceiveChannel<T>
}
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

package com.gitlab.ykrasik.gamedex.app.api.util

import com.gitlab.ykrasik.gamedex.util.IsValid

/**
 * User: ykrasik
 * Date: 17/05/2020
 * Time: 17:43
 *
 * This class exists to work around a feature (or limitation) of StateFlow - value equality.
 * There are a lot of situations where views contain 2 fields - a field with a value, and a field whether that value is valid.
 * However, some presenters need to act on pairs of (value, valueIsValid), so they would zip the 2 flows together.
 * However, if the value was valid, the value was then changed but remained valid, the IsValid flow will not emit a new element,
 * which means the presenters zip will not fire.
 * So instead of an 'IsValid' field, we use a ValidationResult field, which is guaranteed to fire on each value change.
 */
data class ValidatedValue<T>(
    val value: T,
    val isValid: IsValid
)
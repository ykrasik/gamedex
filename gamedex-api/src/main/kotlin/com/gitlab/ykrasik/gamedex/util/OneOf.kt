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

package com.gitlab.ykrasik.gamedex.util

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * User: ykrasik
 * Date: 09/12/2018
 * Time: 13:59
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = OneOf.A::class, name = "a"),
    JsonSubTypes.Type(value = OneOf.B::class, name = "b"),
    JsonSubTypes.Type(value = OneOf.C::class, name = "c")
)
sealed class OneOf<out A, out B, out C> {
    data class A<A>(val value: A) : OneOf<A, Nothing, Nothing>()
    data class B<B>(val value: B) : OneOf<Nothing, B, Nothing>()
    data class C<C>(val value: C) : OneOf<Nothing, Nothing, C>()
}
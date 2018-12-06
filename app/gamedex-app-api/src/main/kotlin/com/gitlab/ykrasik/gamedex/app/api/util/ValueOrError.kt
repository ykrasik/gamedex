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

package com.gitlab.ykrasik.gamedex.app.api.util

/**
 * User: ykrasik
 * Date: 02/12/2018
 * Time: 15:40
 *
 * A value or an error message. It is expected that if [error] == null, [value] != null.
 * Can be used, for example, to disable a button and provide an explanation why it's disabled, or
 * show a validation error.
 */
data class ValueOrError<out T>(val value: T?, val error: String?) {
    val isSuccess: Boolean get() = error == null
    val isError: Boolean get() = error != null

    companion object {
        fun <T> success(value: T) = ValueOrError(value, error = null)
        fun <T> error(error: String) = ValueOrError<T>(value = null, error = error)

        val valid: IsValid = success(Unit)
        fun invalid(error: String): IsValid = error(error)

        fun fromError(error: String?): IsValid = if (error == null) valid else invalid(error)

        inline operator fun <T> invoke(f: () -> T): ValueOrError<T> =
            try {
                success(f())
            } catch (e: Exception) {
                error(e.message!!)
            }
    }
}

typealias IsValid = ValueOrError<Any>

fun IsValid.or(other: IsValid): IsValid = if (isError) other else this
fun IsValid.and(other: IsValid): IsValid = if (isSuccess) other else this
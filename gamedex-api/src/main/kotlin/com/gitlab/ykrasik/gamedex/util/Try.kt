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

package com.gitlab.ykrasik.gamedex.util

/**
 * User: ykrasik
 * Date: 02/12/2018
 * Time: 15:40
 */
sealed class Try<out T> {
    data class Success<out T>(val value: T) : Try<T>()
    data class Error<out T>(val error: Exception) : Try<T>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun get(): T = when (this) {
        is Success -> value
        is Error -> throw error
    }

    val valueOrNull get() = if (this is Success) value else null
    val errorOrNull get() = if (this is Error) error else null

    companion object {
        fun <T> success(value: T): Try<T> = Success(value)
        fun <T> error(error: Exception): Try<T> = Error(error)

        val valid: IsValid = success(Unit)
        fun invalid(error: String): IsValid = error(IllegalArgumentException(error))

        inline operator fun <T> invoke(f: () -> T): Try<T> =
            try {
                success(f())
            } catch (e: Exception) {
                error(e)
            }
    }
}

typealias IsValid = Try<Any>

fun IsValid.or(other: IsValid): IsValid = if (isError) other else this
fun IsValid.and(other: IsValid): IsValid = if (isSuccess) other else this
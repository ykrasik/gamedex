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

package com.gitlab.ykrasik.gamedex.util

/**
 * User: ykrasik
 * Date: 02/12/2018
 * Time: 15:40
 */
sealed class Try<out T> {
    data class Success<out T>(val value: T) : Try<T>()
    data class Failure<out T>(val error: Exception) : Try<T>() {
        override fun hashCode() = error.hashCode()
        override fun equals(other: Any?): Boolean {
            if (other !is Failure<*>) return false
            val otherError = other.error

            // This is not a full-fledged exception checking mechanism but it is quick & good enough for our requirements.
            // It exists so that Failure acts as a data class - this avoids redundant value changes in StateFlow.
            return this.error.isEqual(otherError) && this.error.cause.isEqual(other.error.cause)
            //this.error.stackTrace!!.contentEquals(otherError.stackTrace)
        }

        private fun Throwable?.isEqual(other: Throwable?) = when {
            this == null && other == null -> true
            this != null && other != null -> this::class == other::class && this.message == other.message
            else -> false
        }
    }

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw error
    }

    fun getOrNull() = if (this is Success) value else null
    fun exceptionOrNull() = if (this is Failure) error else null

    companion object {
        fun <T> success(value: T): Try<T> = Success(value)
        fun <T> error(error: Exception): Try<T> = Failure(error)

        val valid: IsValid = success(Unit)
        fun invalid(error: String): IsValid = error(IllegalStateException(error))

        inline operator fun <T> invoke(f: () -> T): Try<T> =
            try {
                success(f())
            } catch (e: Exception) {
                error(e)
            }
    }
}

// TODO: Make this inline when kotlin compiler stops throwing errors
@Suppress("UNCHECKED_CAST")
fun <T, R> Try<T>.map(f: (T) -> R): Try<R> = when (this) {
    is Try.Success -> Try { f(value) }
    is Try.Failure -> this as Try<R>
}

typealias IsValid = Try<Any>

@Suppress("FunctionName")
// FIXME: Once kotlin stops throwing internal compiler errors, make this inline
fun <T> IsValid(f: () -> T): IsValid = Try { f() as Any }

infix fun IsValid.or(other: IsValid): IsValid = if (isFailure) other else this
infix fun IsValid.and(other: IsValid): IsValid = if (isSuccess) other else this
val IsValid.not get(): IsValid = if (isSuccess) IsValid.invalid("Negation") else IsValid.valid
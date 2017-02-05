package com.gitlab.ykrasik.gamedex.provider.util

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.github.ykrasik.gamedex.common.util.fromJson
import com.github.ykrasik.gamedex.common.util.listFromJson

/**
 * User: ykrasik
 * Date: 29/01/2017
 * Time: 13:27
 */
inline fun <reified T : Any> Result<ByteArray, FuelError>.fromJson(): T = when (this) {
    is Result.Failure -> throw error
    is Result.Success -> value.fromJson()
}

inline fun <reified T : Any> Result<ByteArray, FuelError>.listFromJson(): List<T> = when (this) {
    is Result.Failure -> throw error
    is Result.Success -> value.listFromJson<T>()
}
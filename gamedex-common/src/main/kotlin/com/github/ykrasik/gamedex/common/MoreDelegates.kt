package com.github.ykrasik.gamedex.common

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

/**
 * User: ykrasik
 * Date: 12/10/2016
 * Time: 12:54
 */
fun <I, T, R> I.delegate(to: T, f: KProperty1<T, R>) = object : ReadOnlyProperty<I, R> {
    override fun getValue(thisRef: I, property: KProperty<*>): R = f(to)
}
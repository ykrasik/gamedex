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

package com.gitlab.ykrasik.gamdex.core.api.util

import com.gitlab.ykrasik.gamedex.util.Extractor
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlin.reflect.KProperty

/**
 * User: ykrasik
 * Date: 24/03/2018
 * Time: 15:56
 */
lateinit var uiThreadScheduler: Scheduler
lateinit var uiThreadDispatcher: CoroutineDispatcher

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                                  BehaviorSubject                                                   //
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
fun <T> behaviorSubject(t: T? = null): BehaviorSubject<T> = if (t == null) BehaviorSubject.create() else BehaviorSubject.createDefault(t)
fun <T> behaviorSubject(f: () -> T?): BehaviorSubject<T> = behaviorSubject(f())
fun <T> T.toBehaviorSubject(): BehaviorSubject<T> = BehaviorSubject.createDefault(this)

var <T> BehaviorSubject<T>.value_: T
    get() = value
    set(value) {
        if (value != this.value) {
            onNext(value)
        }
    }

fun <T> BehaviorSubject<T>.modifyValue(f: (T) -> T) {
    value_ = f(value_)
}

//fun <T> BehaviorSubject<T>.changes(): Observable<T> =
//    scanWith({ value to value }) { acc, cur -> acc.second to cur }.filter { (old, new) -> old != new }.map { it.second }

fun <T> BehaviorSubject<T>.changes(skipFirst: Boolean = true): Observable<T> = distinctUntilChanged().skip(if (skipFirst) 1 else 0)

fun <T, R> BehaviorSubject<T>.mapSubject(scheduler: Scheduler? = null, extractor: Extractor<T, R>): BehaviorSubject<R> {
    val mapped = behaviorSubject { extractor(this.value) }
    this.changes().observeOnMaybe(scheduler).subscribe {
        val newValue = extractor(it)
        mapped.value_ = newValue
    }
    return mapped
}

// TODO: Write unit tests for this.
fun <T, R> BehaviorSubject<T>.mapBidirectional(extractor: Extractor<T, R>, reverseExtractor: Extractor<R, T>, scheduler: Scheduler? = null): BehaviorSubject<R> {
    val origin = this
    val mapped = behaviorSubject { extractor(this.value) }

    origin.changes().observeOnMaybe(scheduler).subscribe {
        val newValue = extractor(it)
        mapped.value_ = newValue
    }
    mapped.changes().observeOnMaybe(scheduler).subscribe {
        val newValue = reverseExtractor(it)
        origin.value_ = newValue
    }

    return mapped
}

operator fun <T> BehaviorSubject<T>.getValue(thisRef: Any, property: KProperty<*>) = value_
operator fun <T> BehaviorSubject<T>.setValue(thisRef: Any, property: KProperty<*>, value: T) {
    value_ = value
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                                  Observable                                                        //
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
fun <T> Observable<T>.observeOnMaybe(scheduler: Scheduler?): Observable<T> =
    if (scheduler != null) observeOn(scheduler) else this

fun <T, R, U> Observable<T>.combineLatest(other: Observable<R>, mapper: (T, R) -> U): Observable<U> =
    Observables.combineLatest(this, other, mapper)

fun <T> Observable<T>.toBehaviorSubject(): BehaviorSubject<T> {
    val subject = behaviorSubject<T>()
    subscribe {
        subject.value_ = it
    }
    return subject
}

fun <T> Observable<T>.toBehaviorSubjectOnChange(f: (T) -> Unit): BehaviorSubject<T> {
    val subject = toBehaviorSubject()
    subject.changes().subscribe {
        f(it)
    }
    return subject
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                                  PublishSubject                                                    //
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
fun <T> publishSubject(): PublishSubject<T> = PublishSubject.create()

fun <T> PublishSubject<T>.publish(t: T) = onNext(t)

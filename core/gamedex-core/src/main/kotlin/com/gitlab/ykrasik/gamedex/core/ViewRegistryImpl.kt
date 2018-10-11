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

package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.app.api.ViewRegistry
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 26/05/2018
 * Time: 17:33
 */
@Singleton
class ViewRegistryImpl @Inject constructor(initialPresenters: MutableMap<KClass<*>, Presenter<*>>) : ViewRegistry {
    private val presenters: Map<KClass<*>, Presenter<*>> = initialPresenters
    private val activePresentations = mutableMapOf<Any, List<Presentation>>()

    override fun register(view: Any) {
        check(activePresentations.put(view, presentView(view)) == null) { "View already registered: $view" }
    }

    private fun presentView(view: Any): List<Presentation> {
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> Presenter<*>.doPresent() = (this as Presenter<T>).present(view as T)

        return view.javaClass.interfaces.map { iface ->
            presenters[iface.kotlin]!!.doPresent<Any>()
        }
    }

    override fun unregister(view: Any) {
        val presentersToDestroy = checkNotNull(activePresentations.remove(view)) { "View not registered: $view" }
        presentersToDestroy.forEach { it.destroy() }
    }

    override fun onShow(view: Any) {
        println("$view: Showing")
        // TODO: Eventually, make it activePresentations[view]!!
        activePresentations[view]?.forEach { it.show() }
    }

    override fun onHide(view: Any) {
        println("$view: Hidden")
        // TODO: Eventually, make it activePresentations[view]!!
        activePresentations[view]?.forEach { it.hide() }
    }

//    init {
//        Runtime.getRuntime().addShutdownHook(thread(start = false, name = "Shutdown") {
//            runBlocking {
//                activePresentations.values.flatten().forEach { it.destroy() }
//            }
//        })
//    }
}
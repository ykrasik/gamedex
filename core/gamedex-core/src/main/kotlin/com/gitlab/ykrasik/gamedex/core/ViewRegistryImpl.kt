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

package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.app.api.ViewRegistry
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import com.gitlab.ykrasik.gamedex.util.logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses

/**
 * User: ykrasik
 * Date: 26/05/2018
 * Time: 17:33
 */
@Singleton
class ViewRegistryImpl @Inject constructor(
    private val presenters: MutableMap<KClass<*>, Presenter<*>>,
) : ViewRegistry {
    private val log = logger()

    private val sessions = mutableMapOf<Any, List<ViewSession>>()

    override fun onCreate(view: Any) {
        check(sessions.put(view, presentView(view)) == null) { "View already registered: $view" }
    }

    @Suppress("UNCHECKED_CAST")
    private fun presentView(view: Any): List<ViewSession> {
        return view.javaClass.kotlin.allSuperclasses.mapNotNull { klass ->
            (presenters[klass] as? Presenter<Any>)?.present(view)
        }
    }

    override fun onDestroy(view: Any) {
        val presentersToDestroy = checkNotNull(sessions.remove(view)) { "View not registered: $view" }
        presentersToDestroy.forEach { it.destroy() }
    }

    override fun onShow(view: Any) {
        log.trace("Shown: $view")
        view.withSessions { it.onShow() }
    }

    override fun onHide(view: Any) {
        log.trace("Hidden: $view")
        view.withSessions { it.onHide() }
    }

    private inline fun Any.withSessions(f: (ViewSession) -> Unit) {
        val sessions = checkNotNull(this@ViewRegistryImpl.sessions[this]) { "View not registered: $this" }
        sessions.forEach(f)
    }

//    init {
//        Runtime.getRuntime().addShutdownHook(thread(start = false, name = "Shutdown") {
//            runBlocking {
//                activePresentations.values.flatten().forEach { it.destroy() }
//            }
//        })
//    }
}
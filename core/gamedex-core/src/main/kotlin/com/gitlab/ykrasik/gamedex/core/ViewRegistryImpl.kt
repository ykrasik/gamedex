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
import com.gitlab.ykrasik.gamedex.core.filter.presenter.MenuGameFilterPresenter
import com.gitlab.ykrasik.gamedex.core.filter.presenter.ReportGameFilterPresenter
import com.gitlab.ykrasik.gamedex.core.general.*
import com.gitlab.ykrasik.gamedex.core.log.LogEntriesPresenter
import com.gitlab.ykrasik.gamedex.core.log.LogLevelPresenter
import com.gitlab.ykrasik.gamedex.core.log.LogTailPresenter
import com.gitlab.ykrasik.gamedex.core.settings.presenter.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 26/05/2018
 * Time: 17:33
 */
@Singleton
class ViewRegistryImpl @Inject constructor(
    menuGameFilter: MenuGameFilterPresenter,
    reportGameFilter: ReportGameFilterPresenter,

    showSettings: ShowSettingsPresenter,
    settings: SettingsPresenter,

    exportDatabase: ExportDatabasePresenter,
    importDatabase: ImportDatabasePresenter,
    clearUserData: ClearUserDataPresenter,
    cleanupCache: CleanupCachePresenter,
    cleanupData: CleanupDataPresenter,

    changeGameCellDisplaySettings: ChangeGameCellDisplaySettingsPresenter,
    gameCellDisplaySettings: GameCellDisplaySettingsPresenter,
    changeGameNameOverlayDisplaySettings: ChangeGameNameOverlayDisplaySettingsPresenter,
    gameNameOverlayDisplaySettings: GameNameOverlayDisplaySettingsPresenter,
    changeGameMetaTagOverlayDisplaySettings: ChangeGameMetaTagOverlayDisplaySettingsPresenter,
    gameMetaTagOverlayDisplaySettings: GameMetaTagOverlayDisplaySettingsPresenter,
    changeGameVersionOverlayDisplaySettings: ChangeGameVersionOverlayDisplaySettingsPresenter,
    gameVersionOverlayDisplaySettings: GameVersionOverlayDisplaySettingsPresenter,
    providerOrderSettings: ProviderOrderSettingsPresenter,
    providerSettings: ProviderSettingsPresenter,

    logEntries: LogEntriesPresenter,
    logLevel: LogLevelPresenter,
    logTail: LogTailPresenter,

    presenters: MutableMap<KClass<*>, Presenter<*>>
) : ViewRegistry {
    private val presenterClasses: Map<KClass<*>, Presenter<*>> = listOf(
        presenter(menuGameFilter),
        presenter(reportGameFilter),

        presenter(showSettings),
        presenter(settings),

        presenter(exportDatabase),
        presenter(importDatabase),
        presenter(clearUserData),
        presenter(cleanupCache),
        presenter(cleanupData),

        presenter(changeGameCellDisplaySettings),
        presenter(gameCellDisplaySettings),
        presenter(changeGameNameOverlayDisplaySettings),
        presenter(gameNameOverlayDisplaySettings),
        presenter(changeGameMetaTagOverlayDisplaySettings),
        presenter(gameMetaTagOverlayDisplaySettings),
        presenter(changeGameVersionOverlayDisplaySettings),
        presenter(gameVersionOverlayDisplaySettings),
        presenter(providerOrderSettings),
        presenter(providerSettings),

        presenter(logEntries),
        presenter(logLevel),
        presenter(logTail)
    ).toMap() + presenters

    private val activePresentations = mutableMapOf<Any, List<Presentation>>()

    private inline fun <reified V : Any> presenter(presenter: Presenter<V>) = V::class to presenter

    override fun register(view: Any) {
        check(activePresentations.put(view, presentView(view)) == null) { "View already registered: $view" }
    }

    private fun presentView(view: Any): List<Presentation> {
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> Presenter<*>.doPresent() = (this as Presenter<T>).present(view as T)

        return view.javaClass.interfaces.map { iface ->
            presenterClasses[iface.kotlin]!!.doPresent<Any>()
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
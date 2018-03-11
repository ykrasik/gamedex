package com.gitlab.ykrasik.gamedex.settings

import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 11/03/2018
 * Time: 15:12
 */
@Singleton
class AllSettings @Inject constructor(
    val game: GameSettings,
    val gameWall: GameWallSettings,
    val general: GeneralSettings,
    val preloader: PreloaderSettings,
    val provider: ProviderSettings,
    val report: ReportSettings
) {
    private val all = listOf(game, gameWall, general, preloader, provider, report)

    fun saveSnapshot() = all.forEach {
        it.disableWrite()
        it.saveSnapshot()
    }

    fun restoreSnapshot() = all.forEach {
        it.restoreSnapshot()
        it.enableWrite()
        it.clearSnapshot()
    }

    fun commitSnapshot() = all.forEach {
        it.enableWrite()
        it.flush()
        it.clearSnapshot()
    }
}
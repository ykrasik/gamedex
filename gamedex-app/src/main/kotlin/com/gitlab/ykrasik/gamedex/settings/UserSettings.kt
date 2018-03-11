package com.gitlab.ykrasik.gamedex.settings

/**
 * User: ykrasik
 * Date: 11/03/2018
 * Time: 15:03
 */
abstract class UserSettings {
    protected abstract val repo: SettingsRepo<*>

    fun saveSnapshot() = repo.saveSnapshot()
    fun restoreSnapshot() = repo.restoreSnapshot()
    fun clearSnapshot() = repo.clearSnapshot()

    fun disableWrite() = repo.disableWrite()
    fun enableWrite() = repo.enableWrite()

    fun flush() = repo.flush()
}
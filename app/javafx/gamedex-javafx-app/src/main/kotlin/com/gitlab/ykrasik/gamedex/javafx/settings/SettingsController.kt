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

package com.gitlab.ykrasik.gamedex.javafx.settings

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.api.general.GeneralSettingsPresenter
import com.gitlab.ykrasik.gamedex.core.general.GeneralUserConfig
import com.gitlab.ykrasik.gamedex.core.provider.ProviderUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.javafx.fitAtMost
import com.gitlab.ykrasik.gamedex.javafx.subscribe
import com.gitlab.ykrasik.gamedex.javafx.subscribeFx
import com.gitlab.ykrasik.gamedex.javafx.task.JavaFxTaskRunner
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.ProviderUserAccount
import com.gitlab.ykrasik.gamedex.util.browse
import com.gitlab.ykrasik.gamedex.util.info
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.now
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.withContext
import org.joda.time.DateTimeZone
import tornadofx.*
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 09/05/2017
 * Time: 17:09
 */
// TODO: Move to tornadoFx di() and have the presenter as a dependency.
@Singleton
class SettingsController @Inject constructor(
    private val generalSettingsPresenter: GeneralSettingsPresenter,
    private val userConfigRepository: UserConfigRepository,
    taskRunner: JavaFxTaskRunner
) : Controller() {
    private val logger = logger()

    private val settingsView: SettingsView by inject()
    private val generalSettingsView: JavaFxGeneralSettingsView by inject()

    private val providerUserConfig = userConfigRepository[ProviderUserConfig::class]
    private val generalUserConfig = userConfigRepository[GeneralUserConfig::class]

    init {
        taskRunner.canRunTaskProperty.subscribeFx {
            generalSettingsView.canRunTask.send(it)
        }
        generalSettingsView.exportDatabaseEvents.subscribe {
            exportDatabase()
        }
        generalSettingsView.importDatabaseEvents.subscribe {
            importDatabase()
        }
        generalSettingsView.clearUserDataEvents.subscribe {
            clearUserData()
        }
        generalSettingsView.cleanupDbEvents.subscribe {
            cleanupDb()
        }
    }

    fun providerSettings(providerId: ProviderId) = providerUserConfig[providerId]
    fun setProviderEnabled(providerId: ProviderId, enable: Boolean) {
        providerUserConfig.modify(providerId) { copy(enable = enable) }
    }

    fun showSettingsMenu() {
        userConfigRepository.saveSnapshot()
        try {
            val accept = settingsView.show()
            if (accept) {
                userConfigRepository.commitSnapshot()
            } else {
                userConfigRepository.revertSnapshot()
            }
        } catch (e: Exception) {
            logger.error("Error updating settings!", e)
            userConfigRepository.revertSnapshot()
        }
    }

    suspend fun validateAndUseAccount(provider: GameProvider, account: Map<String, String>): Boolean = withContext(CommonPool) {
        val newAccount = provider.accountFeature!!.createAccount(account)
        val valid = validate(provider, newAccount)
        if (valid) {
            withContext(JavaFx) {
                providerUserConfig.modify(provider.id) { copy(account = account) }
            }
        }
        valid
    }

    private fun validate(provider: GameProvider, account: ProviderUserAccount): Boolean = try {
        logger.info { "[${provider.id}] Validating: $account" }
        provider.search("TestSearchToVerifyAccount", Platform.pc, account)
        logger.info { "[${provider.id}] Valid!" }
        true
    } catch (e: Exception) {
        logger.warn("[${provider.id}] Invalid!", e)
        false
    }

    private suspend fun exportDatabase() = withContext(JavaFx) {
        val dir = chooseDirectory("Choose database export directory...", initialDirectory = generalUserConfig.exportDbDirectory) ?: return@withContext
        generalUserConfig.exportDbDirectory = dir
        val timestamp = now.withZone(DateTimeZone.getDefault())
        val timestamptedPath = Paths.get(
            dir.toString(),
            timestamp.toString("yyyy-MM-dd"),
            "db_${timestamp.toString("HH_mm_ss")}.json"
        ).toFile()

        generalSettingsPresenter.exportDatabase(timestamptedPath)
        browse(timestamptedPath.parentFile)
    }

    private suspend fun importDatabase() = withContext(JavaFx) {
        val file = chooseFile("Choose database file...", filters = emptyArray(), mode = FileChooserMode.Single) {
            initialDirectory = generalUserConfig.exportDbDirectory
            initialFileName = "db.json"
        }.firstOrNull() ?: return@withContext

        if (areYouSureDialog("This will overwrite the existing database.")) {
            generalSettingsPresenter.importDatabase(file)
        }
    }

    private suspend fun clearUserData() = withContext(JavaFx) {
        val confirm = areYouSureDialog("Clear game user data?") {
            text("This will remove tags, excluded providers & any custom information entered (like custom names or thumbnails) from all games.") {
                wrappingWidth = 400.0
            }
        }
        if (confirm) {
            generalSettingsPresenter.deleteAllUserData()
        }
    }

    private suspend fun cleanupDb() = withContext(JavaFx) {
        val staleData = generalSettingsPresenter.detectStaleData()
        if (staleData.isEmpty) return@withContext

        val confirm = areYouSureDialog("Delete the following stale data?") {
            if (staleData.libraries.isNotEmpty()) {
                label("Stale Libraries: ${staleData.libraries.size}")
                listview(staleData.libraries.map { it.path }.observable()) { fitAtMost(5) }
            }
            if (staleData.games.isNotEmpty()) {
                label("Stale Games: ${staleData.games.size}")
                listview(staleData.games.map { it.path }.observable()) { fitAtMost(5) }
            }
            if (staleData.images.isNotEmpty()) {
                label("Stale Images: ${staleData.images.size} (${staleData.staleImagesSize})")
                listview(staleData.images.map { it.first }.observable()) { fitAtMost(5) }
            }
        }

        if (confirm) {
            // TODO: Create backup before deleting
            generalSettingsPresenter.deleteStaleData(staleData)
        }
    }
}
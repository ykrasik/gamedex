package com.gitlab.ykrasik.gamedex.settings

import tornadofx.getValue
import tornadofx.setValue
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 09/03/2018
 * Time: 09:28
 */
@Singleton
class PreloaderSettings : UserSettings() {
    override val repo = SettingsRepo("preloader") {
        Data(
            diComponents = 24
        )
    }

    val diComponentsProperty = repo.intProperty(Data::diComponents) { copy(diComponents = it) }
    var diComponents by diComponentsProperty

    data class Data(
        val diComponents: Int
    )
}
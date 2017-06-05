package com.gitlab.ykrasik.gamedex.ui.view.settings

import com.gitlab.ykrasik.gamedex.controller.SettingsController
import com.gitlab.ykrasik.gamedex.ui.jfxButton
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 14:57
 */
class GeneralSettingsView : View("General Settings", Theme.Icon.settings()) {
    private val settingsController: SettingsController by di()

    override val root = form {
        // TODO: Add a 'purge images' button
        fieldset("Database") {
            field {
                jfxButton("Export Database", Theme.Icon.upload()) {
                    addClass(Style.databaseButton)
                    setOnAction { settingsController.exportDatabase() }
                }
            }
            field {
                jfxButton("Import Database", Theme.Icon.download()) {
                    addClass(Style.databaseButton)
                    setOnAction { settingsController.importDatabase() }
                }
            }
        }
    }

    class Style : Stylesheet() {
        companion object {
            val databaseButton by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            databaseButton {
                borderColor = multi(box(Color.BLACK))
                borderRadius = multi(box(3.px))
                borderWidth = multi(box(0.5.px))
            }
        }
    }
}
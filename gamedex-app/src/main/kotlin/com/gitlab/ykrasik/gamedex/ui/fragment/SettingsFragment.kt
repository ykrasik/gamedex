package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.GameProviderType
import com.gitlab.ykrasik.gamedex.controller.SettingsController
import com.gitlab.ykrasik.gamedex.settings.AllSettings
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.settings.ProviderSettings
import com.gitlab.ykrasik.gamedex.ui.*
import javafx.beans.property.Property
import javafx.scene.layout.Pane
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.*

/**
 * User: ykrasik
 * Date: 06/01/2017
 * Time: 22:22
 */
// TODO: Update to be in line with application look,
class SettingsFragment : Fragment("Settings") {
    private val settingsController: SettingsController by di()
    private val settings: AllSettings by di()

    // TODO: Use a viewModel.
    override val root = borderpane {
        center {
            tabpane {
                nonClosableTab("Game Provider") {
                    form {
                        paddingAll = 20
                        // TODO: Consider ControlsFx PropertySheet
                        fieldset("Preferred Provider for Game Data") {
                            listOf(
                                "Search First" to settings.provider.searchOrderProperty,
                                "Name" to settings.provider.nameOrderProperty,
                                "Description" to settings.provider.descriptionOrderProperty,
                                "Release Date" to settings.provider.releaseDateOrderProperty,
                                "Critic Score" to settings.provider.criticScoreOrderProperty,
                                "User Score" to settings.provider.userScoreOrderProperty,
                                "Thumbnail" to settings.provider.thumbnailOrderProperty,
                                "Poster" to settings.provider.posterOrderProperty,
                                "Screenshots" to settings.provider.screenshotOrderProperty
                            ).forEach { (name, preferenceProperty) ->
                                field(name) {
                                    enumComboBox<GameProviderType> {
                                        value = preferenceProperty.get().preferredProvider()
                                        valueProperty().onChange {
                                            preferenceProperty.set(ProviderSettings.Order.prefer(it!!))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                nonClosableTab("Game Display") {
                    form {
                        paddingAll = 20
                        fieldset("Game Display Type") {
                            field("Type") { enumComboBox(settings.game.displayTypeProperty) }
                        }

                        fieldset("Game Wall") {
                            visibleWhen { settings.game.displayTypeProperty.isEqualTo(GameSettings.DisplayType.wall) }
                            field("Cell Image Display") { enumComboBox(settings.gameWall.imageDisplayTypeProperty) }
                            // TODO: Should probably validate this data.
                            field("Cell Width") { adjustableTextField(settings.gameWall.cellWidthProperty) }
                            field("Cell Height") { adjustableTextField(settings.gameWall.cellHeightProperty) }
                            field("Cell Horizontal Spacing") { adjustableTextField(settings.gameWall.cellHorizontalSpacingProperty) }
                            field("Cell Vertical Spacing") { adjustableTextField(settings.gameWall.cellVerticalSpacingProperty) }
                        }
                    }
                }
                nonClosableTab("General") {
                    vbox(spacing = 10.0) {
                        paddingAll = 20
                        button("Export Database") {
                            setOnAction {
                                settingsController.exportDatabase()
                            }
                        }
                        button("Import Database") {
                            setOnAction {
                                settingsController.importDatabase()
                            }
                        }
                        // TODO: Add a 'purge images' button
                    }
                }
            }
        }
        bottom {
            buttonbar {
                paddingAll = 20
                cancelButton { setOnAction { close() } }
                okButton { setOnAction { close() } }
            }
        }
    }

    private fun Pane.adjustableTextField(property: Property<Double>) {
        val textfield = textfield(property)

        fun adjustButton(icon: FontAwesome.Glyph, adjustment: Int) {
            button(graphic = icon.toGraphic()) {
                setOnAction {
                    textfield.text = ((textfield.text).toDouble() + adjustment).toString()
                }
            }
        }

        adjustButton(FontAwesome.Glyph.PLUS, +1)
        adjustButton(FontAwesome.Glyph.MINUS, -1)
    }

    fun show() {
        openModal(block = true)
    }
}
//fun SettingsScreen(@NonNull configService: ConfigService, @NonNull stageManager: StageManager): ??? {
//    this.configService = configService
//    this.stageManager = stageManager
//
//    val loader = FXMLLoader(UIResources.settingsScreenFxml())
//    loader.setController(this)
//    val root = loader.load()
//
//    val scene = Scene(root, Color.TRANSPARENT)
//    scene.stylesheets.addAll(UIResources.mainCss(), UIResources.settingsScreenCss())
//
//    stage.setWidth(600.0)
//    stage.initStyle(StageStyle.TRANSPARENT)
//    stage.initModality(Modality.APPLICATION_MODAL)
//    stage.setScene(scene)
//
//    // Make the stage draggable by clicking anywhere.
//    JavaFxUtils.makeStageDraggable(stage, root)
//}
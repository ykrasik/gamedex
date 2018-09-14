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

package com.gitlab.ykrasik.gamedex.app.javafx.settings

import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderAccountState
import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderSettingsView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * User: ykrasik
 * Date: 06/03/2018
 * Time: 10:02
 */
class JavaFxProviderSettingsView(override val provider: GameProvider) : PresentableView(), ProviderSettingsView {
    override var providerLogos = emptyMap<ProviderId, Image>()

    private val stateProperty = SimpleObjectProperty(ProviderAccountState.Empty)
    override var state by stateProperty

    private val checkingAccountProperty = SimpleBooleanProperty(false)
    override var isCheckingAccount by checkingAccountProperty
    override var lastVerifiedAccount = emptyMap<String, String>()

    override val currentAccountChanges = channel<Map<String, String>>()
    private val currentAccountProperty = SimpleObjectProperty(emptyMap<String, String>()).eventOnChange(currentAccountChanges)
    override var currentAccount by currentAccountProperty

    override val enabledChanges = channel<Boolean>()
    private val providerEnabledProperty = SimpleBooleanProperty(false).eventOnChange(enabledChanges)
    override var enabled by providerEnabledProperty

    override val accountUrlClicks = channel<Unit>()

    override val verifyAccountRequests = channel<Unit>()

    private var accountLabelFlashContainer: StackPane by singleAssign()

    init {
        viewRegistry.register(this)
    }

    override val root = vbox {
        hbox {
            alignment = Pos.CENTER_LEFT
            paddingAll = 10
            label(provider.id) { addClass(Style.providerLabel) }
            spacer()
            imageview {
                fitHeight = 60.0
                isPreserveRatio = true
                image = providerLogos[provider.id]!!.image
            }
        }
        separator()
        stackpane {
            form {
                fieldset {
                    field("Enable") {
                        hbox {
                            alignment = Pos.BASELINE_CENTER
                            jfxToggleButton(providerEnabledProperty)
                            spacer()
                            stackpane {
                                visibleWhen { stateProperty.isNotEqualTo(ProviderAccountState.NotRequired) }
                                label {
                                    addClass(Style.accountLabel)
                                    textProperty().bind(stateProperty.map { it!!.text })
                                    graphicProperty().bind(stateProperty.map { it!!.graphic })
                                    textFillProperty().bind(stateProperty.map { it!!.color })
                                }
                                accountLabelFlashContainer = stackpane { addClass(Style.flashContainer) }
                            }
                        }
                    }
                }
                val accountFeature = provider.accountFeature
                if (accountFeature != null) {
                    fieldset("Account") {
                        accountField(accountFeature.field1)
                        accountField(accountFeature.field2)
                        accountField(accountFeature.field3)
                        accountField(accountFeature.field4)
                        hbox {
                            spacer()
                            jfxButton("Verify Account") {
                                addClass(CommonStyle.toolbarButton, CommonStyle.acceptButton, CommonStyle.thinBorder)
                                disableWhen { stateProperty.isEqualTo(ProviderAccountState.Empty) }
                                isDefaultButton = true
                                eventOnAction(verifyAccountRequests)
                            }
                        }
                        field("Create") {
                            hyperlink(accountFeature.accountUrl) {
                                eventOnAction(accountUrlClicks)
                            }
                        }
                    }
                }
            }
            maskerPane(checkingAccountProperty)
        }
    }

    private fun Fieldset.accountField(field: String?) {
        if (field == null) return
        field(field) {
            val currentValue = currentAccountProperty.map { account -> account!![field] ?: "" }
            textfield(currentValue) {
                textProperty().onChange {
                    currentAccount += field to it!!
                }
            }
        }
    }

    override fun onInvalidAccount() {
        accountLabelFlashContainer.flash(target = 0.5, reverse = true)
    }

    private val ProviderAccountState.text
        get() = when (this) {
            ProviderAccountState.Valid -> "Valid Account"
            ProviderAccountState.Invalid -> "Invalid Account"
            ProviderAccountState.Empty -> "No Account"
            ProviderAccountState.Unverified -> "Unverified Account"
            ProviderAccountState.NotRequired -> null
        }

    private val ProviderAccountState.graphic
        get() = when (this) {
            ProviderAccountState.Valid -> Theme.Icon.accept()
            ProviderAccountState.Invalid -> Theme.Icon.cancel()
            ProviderAccountState.Empty, ProviderAccountState.Unverified -> Theme.Icon.exclamationTriangle().apply { color(Color.ORANGE) }
            ProviderAccountState.NotRequired -> null
        }

    private val ProviderAccountState.color
        get() = when (this) {
            ProviderAccountState.Valid -> Color.GREEN
            ProviderAccountState.Invalid -> Color.INDIANRED
            ProviderAccountState.Empty, ProviderAccountState.Unverified -> Color.ORANGE
            ProviderAccountState.NotRequired -> null
        }

    class Style : Stylesheet() {
        companion object {
            val providerLabel by cssclass()
            val accountLabel by cssclass()
            val flashContainer by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            providerLabel {
                fontSize = 20.px
                fontWeight = FontWeight.BOLD
            }

            accountLabel {
                fontSize = 20.px
                fontWeight = FontWeight.BOLD
                padding = box(5.px)
            }

            flashContainer {
                backgroundColor = multi(Color.RED)
                backgroundRadius = multi(box(5.px))
                borderRadius = multi(box(5.px))
                opacity = 0.0
            }
        }
    }
}
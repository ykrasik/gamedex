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

import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.provider.logoImage
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderUserAccountFeature
import com.gitlab.ykrasik.gamedex.util.browseToUrl
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * User: ykrasik
 * Date: 06/03/2018
 * Time: 10:02
 */
class ProviderUserSettingsFragment(private val provider: GameProvider) : Fragment() {
    private val controller: SettingsController by di()

    private val settings = controller.providerSettings(provider.id)

    private val accountRequired = provider.accountFeature != null
    private val checking = false.toProperty()

    // FIXME: This doesn't update when settings are reset to default.
    private val stateProperty = when {
        !accountRequired -> State.NotRequired
        settings.account != null -> State.Valid
        else -> State.Empty
    }.toProperty()
    private var state by stateProperty

    private var currentAccount = mapOf<String, String>()
    private var lastVerifiedAccount = settings.account ?: emptyMap()

    override val root = vbox {
        hbox {
            alignment = Pos.CENTER_LEFT
            paddingRight = 10
            paddingBottom = 10
            label(provider.id) {
                addClass(Style.providerLabel)
            }
            spacer()
            children += provider.logoImage.toImageView {
                fitHeight = 60.0
                isPreserveRatio = true
            }
        }
        separator()
        stackpane {
            form {
                disableWhen { checking }
                lateinit var accountLabelFlashContainer: Node
                fieldset {
                    field("Enable") {
                        hbox {
                            alignment = Pos.BASELINE_CENTER
                            jfxToggleButton {
                                isSelected = settings.enable
                                selectedProperty().onChange { selected ->
                                    if (selected && state.isValid || !selected) {
                                        controller.setProviderEnabled(provider.id, enable = selected)
                                    } else {
                                        isSelected = false
                                        accountLabelFlashContainer.flash(target = 0.5, reverse = true)
                                    }
                                }
                            }
                            spacer()
                            stackpane {
                                isVisible = accountRequired
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
                provider.accountFeature?.let { accountFeature ->
                    fieldset("Account") {
                        accountFields(accountFeature)
                        field("Create") {
                            hyperlink(accountFeature.accountUrl) {
                                setOnAction { accountFeature.accountUrl.browseToUrl() }
                            }
                        }
                    }
                }
            }
            maskerPane {
                visibleWhen { checking }
            }
        }
    }

    private fun Fieldset.accountFields(accountFeature: ProviderUserAccountFeature) {
        accountFeature.fields.forEach { name ->
            field(name) {
                val currentValue = settings.account?.get(name) ?: ""
                currentAccount += name to currentValue
                textfield(currentValue) {
                    textProperty().onChange {
                        currentAccount += name to it!!
                        state = when {
                            it.isEmpty() -> State.Empty
                            currentAccount == lastVerifiedAccount -> State.Valid
                            else -> State.Unverified
                        }
                    }
                }
            }
        }
        hbox {
            spacer()
            jfxButton("Verify Account") {
                addClass(CommonStyle.toolbarButton, CommonStyle.acceptButton, CommonStyle.thinBorder)
                disableWhen { stateProperty.isEqualTo(State.Empty) }
                isDefaultButton = true
                setOnAction {
                    javaFx {
                        checking.value = true
                        try {
                            val valid = controller.validateAndUseAccount(provider, currentAccount)
                            state = if (valid) {
                                lastVerifiedAccount = currentAccount
                                State.Valid
                            } else {
                                State.Invalid
                            }
                        } finally {
                            checking.value = false
                        }
                    }
                }
            }
        }
    }

    override fun onDock() {
        state = when {
            !accountRequired -> State.NotRequired
            currentAccount.any { it.value.isEmpty() } -> State.Empty
            settings.account != null -> State.Valid
            else -> State.Unverified
        }
        lastVerifiedAccount = settings.account ?: emptyMap()
    }

    private enum class State {
        Valid {
            override val isValid = true
            override val text = "Valid Account"
            override val graphic get() = Theme.Icon.accept()
            override val color = Color.GREEN
        },
        Invalid {
            override val isValid = false
            override val text = "Invalid Account"
            override val graphic get() = Theme.Icon.cancel()
            override val color = Color.INDIANRED
        },
        Empty {
            override val isValid = false
            override val text = "No Account"
            override val graphic get() = Theme.Icon.exclamationTriangle().apply { color(Color.ORANGE) }
            override val color = Color.ORANGE
        },
        Unverified {
            override val isValid = false
            override val text = "Unverified Account"
            override val graphic get() = Theme.Icon.exclamationTriangle().apply { color(Color.ORANGE) }
            override val color = Color.ORANGE
        },
        NotRequired {
            override val isValid = true
            override val text = null
            override val graphic = null
            override val color = null
        };

        abstract val isValid: Boolean
        abstract val text: String?
        abstract val graphic: Node?
        abstract val color: Color?
    }

    class Style : Stylesheet() {
        companion object {
            val providerLabel by cssclass()
            val accountLabel by cssclass()
            val flashContainer by cssclass()

            init {
                importStylesheet(Style::class)
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
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

import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderAccountState
import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderSettingsView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.image.JavaFxImage
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableView
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.ProviderUserAccountFeature
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
class ProviderUserSettingsFragment : PresentableView(), ProviderSettingsView {
    override var providerLogos = emptyMap<ProviderId, Image>()

    private val providerProperty = SimpleObjectProperty<GameProvider>()
    override var provider by providerProperty

    private val stateProperty = SimpleObjectProperty(ProviderAccountState.Empty)
    override var state by stateProperty

    private val checkingAccountProperty = SimpleBooleanProperty(false)
    override var isCheckingAccount by checkingAccountProperty
    override var lastVerifiedAccount = emptyMap<String, String>()

    override var currentAccount = emptyMap<String, String>()
    override val currentAccountChanges = channel<Map<String, String>>()

    override val enabledChanges = channel<Boolean>()
    private val providerEnabledProperty = SimpleBooleanProperty(false).eventOnChange(enabledChanges)
    override var enabled by providerEnabledProperty

    override val accountUrlClicks = channel<Unit>()

    override val verifyAccountRequests = channel<Unit>()

    private var accountLabelFlashContainer: StackPane by singleAssign()

//    private val settings = controller.providerSettings(provider.id)

//    private val accountRequired = provider.accountFeature != null
//    private val checking = false.toProperty()

    // FIXME: This doesn't update when settings are reset to default.
//    private val stateProperty = when {
//        !accountRequired -> State.NotRequired
//        settings.account != null -> State.Valid
//        else -> State.Empty
//    }.toProperty()
//    private var state by stateProperty

//    private var currentAccount = mapOf<String, String>()
//    private var lastVerifiedAccount = settings.account ?: emptyMap()

    override val root = vbox {
        hbox {
            alignment = Pos.CENTER_LEFT
            paddingRight = 10
            paddingBottom = 10
            label(provider.id) {
                addClass(Style.providerLabel)
            }
            spacer()
            children += (providerLogos[provider.id]!! as JavaFxImage).image.toImageView {
                fitHeight = 60.0
                isPreserveRatio = true
            }
        }
        separator()
        stackpane {
            stackpane {
                providerProperty.onChange { provider ->
                    replaceChildren {
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
                            provider!!.accountFeature?.let { accountFeature ->
                                fieldset("Account") {
                                    accountFields(accountFeature)
                                    field("Create") { hyperlink(accountFeature.accountUrl) { eventOnAction(accountUrlClicks) } }
                                }
                            }
                        }
                    }
                }
            }
            maskerPane(checkingAccountProperty)
        }
    }

    private fun Fieldset.accountFields(accountFeature: ProviderUserAccountFeature) {
        accountFeature.fields.forEach { name ->
            field(name) {
                val currentValue = currentAccount[name] ?: ""
//                currentAccount += name to currentValue
                textfield(currentValue) {
                    textProperty().onChange {
                        currentAccount += name to it!!
                        currentAccountChanges.offer(currentAccount)
                    }
                }
            }
        }
        hbox {
            spacer()
            jfxButton("Verify Account") {
                addClass(CommonStyle.toolbarButton, CommonStyle.acceptButton, CommonStyle.thinBorder)
                disableWhen { stateProperty.isEqualTo(ProviderAccountState.Empty) }
                isDefaultButton = true
                eventOnAction(verifyAccountRequests)
            }
        }
    }

    override fun onInvalidAccount() {
        accountLabelFlashContainer.flash(target = 0.5, reverse = true)
    }

//    override fun onDock() {
//        state = when {
//            !accountRequired -> State.NotRequired
//            currentAccount.any { it.value.isEmpty() } -> State.Empty
//            settings.account != null -> State.Valid
//            else -> State.Unverified
//        }
//        lastVerifiedAccount = settings.account ?: emptyMap()
//    }

    private val ProviderAccountState.text get() = when(this) {
        ProviderAccountState.Valid -> "Valid Account"
        ProviderAccountState.Invalid -> "Invalid Account"
        ProviderAccountState.Empty -> "No Account"
        ProviderAccountState.Unverified -> "Unverified Account"
        ProviderAccountState.NotRequired -> null
    }

    private val ProviderAccountState.graphic get() = when(this) {
        ProviderAccountState.Valid -> Theme.Icon.accept()
        ProviderAccountState.Invalid -> Theme.Icon.cancel()
        ProviderAccountState.Empty, ProviderAccountState.Unverified -> Theme.Icon.exclamationTriangle().apply { color(Color.ORANGE) }
        ProviderAccountState.NotRequired -> null
    }

    private val ProviderAccountState.color get() = when(this) {
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
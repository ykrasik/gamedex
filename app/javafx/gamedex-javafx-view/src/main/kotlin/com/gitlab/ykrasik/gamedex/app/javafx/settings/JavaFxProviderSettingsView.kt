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
import com.gitlab.ykrasik.gamedex.app.api.image.ViewWithProviderLogos
import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderAccountStatus
import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderSettingsView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.IsValid
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
class JavaFxProviderSettingsView(override val provider: GameProvider) : PresentableView(), ProviderSettingsView, ViewWithProviderLogos {
    override var providerLogos = emptyMap<ProviderId, Image>()

    override var status = state(ProviderAccountStatus.Empty)
    override var enabled = userMutableState(false)

    override var currentAccount = userMutableState(emptyMap<String, String>())

    override val gotoAccountUrlActions = channel<Unit>()

    override var canVerifyAccount = state(IsValid.valid)

    override val verifyAccountActions = channel<Unit>()

    private var accountLabelFlashContainer: StackPane by singleAssign()

    init {
        register()

        status.property.onChange { status ->
            if (status == ProviderAccountStatus.Invalid) {
                accountLabelFlashContainer.flash(target = 0.5, reverse = true)
            }
        }
    }

    override val root = vbox {
        defaultHbox(alignment = Pos.TOP_LEFT) {
            paddingAll = 5
            label(provider.id) { addClass(Style.providerLabel) }
            spacer()
            imageview {
                fitHeight = 60.0
                isPreserveRatio = true
                image = providerLogos[provider.id]!!.image
            }
        }
        verticalGap(size = 40)
        form {
            fieldset {
                horizontalField("Enable") {
                    hbox(alignment = Pos.BASELINE_CENTER) {
                        jfxToggleButton(enabled.property)
                        spacer()
                        stackpane {
                            visibleWhen { status.property.isNotEqualTo(ProviderAccountStatus.NotRequired) }
                            label {
                                addClass(Style.accountLabel)
                                textProperty().bind(status.property.map { it!!.text })
                                graphicProperty().bind(status.property.map { it!!.icon })
                                textFillProperty().bind(status.property.map { it!!.color })
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
                        confirmButton("Verify Account") {
                            addClass(CommonStyle.thinBorder)
                            enableWhen(canVerifyAccount)
                            isDefaultButton = true
                            eventOnAction(verifyAccountActions)
                        }
                    }
                    horizontalField("Create") {
                        hyperlink(accountFeature.accountUrl) {
                            eventOnAction(gotoAccountUrlActions)
                        }
                    }
                }
            }
        }
    }

    private fun Fieldset.accountField(field: String?) {
        if (field == null) return
        horizontalField(field) {
            val currentValue = currentAccount.property.map { account -> account!![field] ?: "" }
            jfxTextField(currentValue) {
                textProperty().onChange {
                    currentAccount.valueFromView += field to it!!
                }
            }
        }
    }

    private val ProviderAccountStatus.text
        get() = when (this) {
            ProviderAccountStatus.Valid -> "Valid Account"
            ProviderAccountStatus.Invalid -> "Invalid Account"
            ProviderAccountStatus.Empty -> "No Account"
            ProviderAccountStatus.Unverified -> "Unverified Account"
            ProviderAccountStatus.NotRequired -> null
        }

    private val ProviderAccountStatus.icon
        get() = when (this) {
            ProviderAccountStatus.Valid -> Icons.valid
            ProviderAccountStatus.Invalid -> Icons.invalid
            ProviderAccountStatus.Empty, ProviderAccountStatus.Unverified -> Icons.unverified
            ProviderAccountStatus.NotRequired -> null
        }

    private val ProviderAccountStatus.color
        get() = when (this) {
            ProviderAccountStatus.Valid -> Color.GREEN
            ProviderAccountStatus.Invalid -> Color.INDIANRED
            ProviderAccountStatus.Empty, ProviderAccountStatus.Unverified -> Color.ORANGE
            ProviderAccountStatus.NotRequired -> null
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
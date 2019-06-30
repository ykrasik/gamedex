/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderAccountStatus
import com.gitlab.ykrasik.gamedex.app.api.settings.ProviderSettingsView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.api.web.ViewCanBrowseUrl
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.Colors
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.confirmButton
import com.gitlab.ykrasik.gamedex.javafx.theme.logo
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableTabView
import com.gitlab.ykrasik.gamedex.provider.GameProviderMetadata
import com.gitlab.ykrasik.gamedex.util.IsValid
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
class JavaFxProviderSettingsView(override val provider: GameProviderMetadata, icon: Node) : PresentableTabView(provider.id, icon),
    ProviderSettingsView,
    ViewCanBrowseUrl {

    private val commonOps: JavaFxCommonOps by di()

    override val canChangeProviderSettings = state(IsValid.valid)

    override val status = state(ProviderAccountStatus.Empty)
    override val enabled = userMutableState(false)

    override val currentAccount = userMutableState(emptyMap<String, String>())

    override val browseUrlActions = channel<String>()

    override val canVerifyAccount = state(IsValid.valid)

    override val verifyAccountActions = channel<Unit>()

    private val accountLabelContainer = stackpane {
        addClass(Style.flashContainer)
        opacity = 0.0
    }

    init {
        register()

        status.onInvalidated { status ->
            val (toAdd, toRemove) = when (status) {
                ProviderAccountStatus.Valid -> Style.validAccountContainer to listOf(Style.unverifiedAccountContainer, Style.invalidAccountContainer)
                ProviderAccountStatus.Unverified -> Style.unverifiedAccountContainer to listOf(Style.validAccountContainer, Style.invalidAccountContainer)
                ProviderAccountStatus.Invalid -> Style.invalidAccountContainer to listOf(Style.unverifiedAccountContainer, Style.validAccountContainer)
                else -> null to listOf(Style.validAccountContainer, Style.unverifiedAccountContainer, Style.invalidAccountContainer)
            }
            if (toAdd != null) accountLabelContainer.addClass(toAdd)
            accountLabelContainer.removeClass(*toRemove.toTypedArray())
            accountLabelContainer.flashFade(duration = 0.2.seconds, target = 0.5)
        }
    }

    override val root = vbox {
        // Need to wrap in another vbox because the root's disabledProperty cannot be bound,
        // it is set by the tabPane which contains this view.
        errorTooltip(canChangeProviderSettings)
        vbox {
            enableWhen(canChangeProviderSettings, wrapInErrorTooltip = false)
            defaultHbox(alignment = Pos.TOP_LEFT) {
                paddingAll = 5
                label(provider.id) { addClass(Style.providerLabel) }
                spacer()
                vbox(spacing = 5) {
                    children += commonOps.providerLogo(provider.id).toImageView(height = 80.0)
                    defaultHbox(alignment = Pos.CENTER_RIGHT) {
                        provider.supportedPlatforms.reversed().forEach { platform ->
                            add(platform.logo)
                        }
                    }
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
                                    textProperty().bind(status.property.stringBinding { it!!.text })
                                    graphicProperty().bind(status.property.binding { it.icon })
                                    textFillProperty().bind(status.property.binding { it.color })
                                }
                                add(accountLabelContainer)
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
                                setId(Style.verifyAccountButton)
                                enableWhen(canVerifyAccount)
                                isDefaultButton = true
                                action(verifyAccountActions)
                            }
                        }
                        horizontalField {
                            hyperlink(accountFeature.accountUrl, graphic = Icons.link) {
                                action(browseUrlActions) { accountFeature.accountUrl }
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        // This view must call init manually because it is not created via 'inject'
        init()
    }

    private fun Fieldset.accountField(field: String?) {
        if (field == null) return
        horizontalField(field) {
            val currentValue = currentAccount.property.map { it[field] ?: "" }
            jfxTextField(currentValue, promptText = "Enter $field...") {
                textProperty().typeSafeOnChange {
                    currentAccount.valueFromView += field to it
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
            ProviderAccountStatus.Valid -> Colors.green
            ProviderAccountStatus.Invalid -> Colors.red
            ProviderAccountStatus.Empty, ProviderAccountStatus.Unverified -> Colors.orange
            ProviderAccountStatus.NotRequired -> null
        }

    class Style : Stylesheet() {
        companion object {
            val providerLabel by cssclass()
            val accountLabel by cssclass()
            val flashContainer by cssclass()
            val validAccountContainer by cssclass()
            val unverifiedAccountContainer by cssclass()
            val invalidAccountContainer by cssclass()
            val verifyAccountButton by cssid()

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
                backgroundRadius = multi(box(5.px))
                borderRadius = multi(box(5.px))
            }

            validAccountContainer {
                backgroundColor = multi(Colors.green)
            }

            unverifiedAccountContainer {
                backgroundColor = multi(Colors.orange)
            }

            invalidAccountContainer {
                backgroundColor = multi(Colors.red)
            }

            verifyAccountButton {
                borderColor = multi(box(Color.BLACK))
                borderRadius = multi(box(3.px))
                borderWidth = multi(box(0.5.px))
            }
        }
    }
}
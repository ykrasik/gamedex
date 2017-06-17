package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.ReportRule
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.*
import com.gitlab.ykrasik.gamedex.ui.widgets.adjustableTextField
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 16:56
 */
class ViolationRulesFragment(currentRules: ReportRule) : Fragment("Violation Rules") {
    private val rulesProperty = SimpleObjectProperty(currentRules)
    private var rules by rulesProperty

    private var accept = false

    private var rulesContainer: VBox by singleAssign()

    private var indent = 0

    override val root = borderpane {
        addClass(Style.ruleWindow)
        top {
            toolbar {
                acceptButton { setOnAction { close(accept = true) } }
                verticalSeparator()
                toolbarButton(graphic = Theme.Icon.clear()) {
                    tooltip("Clear rules")
                    setOnAction {
                        rules = ReportRule.Nop()
                        close(accept = true)
                    }
                }
                verticalSeparator()
                spacer()
                verticalSeparator()
                cancelButton { setOnAction { close(accept = false) } }
            }
        }
        center {
            vbox {
                addClass(Style.rulesContent)
                rulesContainer = vbox(spacing = 10) {
                    rulesProperty.perform { rules ->
                        // TODO: This is probably leaking a lot of listeners.
                        replaceChildren {
                            render(rules)
                        }
                    }
                }
            }
        }
    }

    private fun EventTarget.render(rule: ReportRule) {
        if (rule is ReportRule.CompositeRule) {
            renderCompositeRule(rule)
        } else {
            renderBasicRule(rule)
        }
    }

    private fun EventTarget.renderCompositeRule(rule: ReportRule.CompositeRule) = vbox(spacing = 2.0) {
        addClass(Style.borderedContent)
        renderBasicRule(rule)

        indent += 1
        render(rule.left)
        render(rule.right)
        indent -= 1
    }

    private fun EventTarget.renderBasicRule(rule: ReportRule) = hbox {
        addClass(CommonStyle.fillAvailableWidth)
        spacing = 5.0
        alignment = Pos.CENTER_LEFT

        region { minWidth = indent * 10.0 }

        val currentRule = rule.toProperty()

        val possibleRules = ReportRule.rules.keys.toList().observable()
        val selectedRuleProperty = SimpleStringProperty(currentRule.value.name)
        popoverComboMenu(
            possibleItems = possibleRules,
            selectedItemProperty = selectedRuleProperty,
            styleClass = Style.ruleButton,
            text = { it },
            menuOp = { if (it == "User Score") separator() }
        )

        selectedRuleProperty.onChange { currentRule.value = ReportRule.rules[it]!!() }

        currentRule.addListener { _, oldValue, newValue ->
            rules = rules.replace(oldValue, newValue)
        }

        platformRule(currentRule)
        criticScoreRule(currentRule)
        userScoreRule(currentRule)

        spacer()

        jfxButton(graphic = Theme.Icon.delete()) {
            disableWhen { currentRule.isEqualTo(rulesProperty) }
            setOnAction {
                rules = rules.delete(currentRule.value)
            }
        }
    }

    private fun HBox.platformRule(currentRule: ObjectProperty<ReportRule>) {
        val platform = currentRule.mapBidirectional(
            { (it as? ReportRule.PlatformFilter)?.platform ?: Platform.pc }, { ReportRule.PlatformFilter(it!!) }
        )
        platformComboBox(platform).apply { showWhen { currentRule.map { it is ReportRule.PlatformFilter } } }
    }

    private fun HBox.criticScoreRule(currentRule: ObjectProperty<ReportRule>) = hbox {
        alignment = Pos.CENTER_LEFT
        val value = currentRule.mapBidirectional(
            { (it as? ReportRule.CriticScore)?.min ?: 65.0 }, { ReportRule.CriticScore(it!!) }
        )
        adjustableTextField(value, "Critic Score", min = 0.0, max = 100.0)
    }.apply { showWhen { currentRule.map { it is ReportRule.CriticScore } } }

    private fun HBox.userScoreRule(currentRule: ObjectProperty<ReportRule>) = hbox {
        alignment = Pos.CENTER_LEFT
        val value = currentRule.mapBidirectional(
            { (it as? ReportRule.UserScore)?.min ?: 65.0 }, { ReportRule.UserScore(it!!) }
        )
        adjustableTextField(value, "User Score", min = 0.0, max = 100.0)
    }.apply { showWhen { currentRule.map { it is ReportRule.UserScore } } }

    fun show(): ReportRule? {
        openWindow(block = true, owner = null)
        return if (accept) rulesProperty.value else null
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    init {
        rulesProperty.onChange { println(it) }
    }

    class Style : Stylesheet() {
        companion object {
            val ruleWindow by cssclass()
            val rulesContent by cssclass()
            val ruleButton by cssclass()
            val borderedContent by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            ruleWindow {
                minWidth = 600.px
                minHeight = 600.px
            }

            rulesContent {
                padding = box(20.px)
            }

            ruleButton {
                minWidth = 80.px
                alignment = Pos.CENTER_LEFT
            }

            borderedContent {
                contentDisplay = ContentDisplay.TOP
                padding = box(2.px)
                borderColor = multi(box(Color.BLACK))
                borderWidth = multi(box(1.px))
            }
        }
    }
}
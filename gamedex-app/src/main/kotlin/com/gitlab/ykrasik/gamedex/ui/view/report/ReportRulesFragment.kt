package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.core.ReportConfig
import com.gitlab.ykrasik.gamedex.core.ReportRule
import com.gitlab.ykrasik.gamedex.ui.buttonWithPopover
import com.gitlab.ykrasik.gamedex.ui.jfxButton
import com.gitlab.ykrasik.gamedex.ui.popoverComboMenu
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.view.report.ReportRuleRenderer.criticScoreRule
import com.gitlab.ykrasik.gamedex.ui.view.report.ReportRuleRenderer.platformFilter
import com.gitlab.ykrasik.gamedex.ui.view.report.ReportRuleRenderer.userScoreRule
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
 * Date: 18/06/2017
 * Time: 14:34
 */
class ReportRulesFragment(
    private val all: Map<String, () -> ReportRule>,
    private val defaultValue: () -> ReportRule,
    private val modifier: ConfigModifier,
    reportConfigProperty: ObjectProperty<ReportConfig>,
    private val rule: ReportRule
) : Fragment() {

    private var reportConfig by reportConfigProperty
    private var indent = 0

    override val root = vbox {
        render(rule)
    }

    private fun EventTarget.render(rule: ReportRule) {
        when (rule) {
            is ReportRule.Operators.BinaryOperator -> renderOperator(rule) {
                render(rule.left)
                render(rule.right)
            }
            is ReportRule.Operators.UnaryOperator -> renderOperator(rule) {
                render(rule.rule)
            }
            is ReportRule.Filters.Filter -> renderBasicRule(rule, all) { currentRule ->
                platformFilter(currentRule)
            }
            is ReportRule.Rules.Rule -> renderBasicRule(rule, all) { currentRule ->
                criticScoreRule(currentRule)
                userScoreRule(currentRule)
            }
        }
    }

    private fun EventTarget.renderOperator(operator: ReportRule.Operators.Operator, op: VBox.() -> Unit) = vbox(spacing = 2.0) {
        addClass(Style.borderedContent)
        renderBasicRule(operator, ReportRule.Operators.all)

        indent += 1
        op(this)
        indent -= 1
    }

    private fun EventTarget.renderBasicRule(rule: ReportRule,
                                            all: Map<String, () -> ReportRule>,
                                            op: (HBox.(SimpleObjectProperty<ReportRule>) -> Unit)? = null) = hbox {
        addClass(CommonStyle.fillAvailableWidth)
        spacing = 5.0
        alignment = Pos.CENTER_LEFT

        region { minWidth = indent * 10.0 }

        val currentRule = rule.toProperty()

        val possibleRules = all.keys.toList().observable()
        val selectedRuleProperty = SimpleStringProperty(currentRule.value.name)

        popoverComboMenu(
            possibleItems = possibleRules,
            selectedItemProperty = selectedRuleProperty,
            styleClass = Style.ruleButton,
            text = { it }
        )

        selectedRuleProperty.onChange { currentRule.value = all[it]!!() }

        currentRule.addListener { _, oldValue, newValue -> replaceRule(oldValue, newValue) }

        op?.invoke(this, currentRule)

        spacer()

        operatorSelection(currentRule)
        jfxButton(graphic = Theme.Icon.delete()) { setOnAction { deleteRule(currentRule.value) } }
    }

    private fun HBox.operatorSelection(currentRule: SimpleObjectProperty<ReportRule>) =
        buttonWithPopover(graphic = Theme.Icon.plus(), styleClass = null) {
            fun operatorButton(name: String, f: (ReportRule) -> ReportRule) = jfxButton(name) {
                addClass(CommonStyle.fillAvailableWidth)
                setOnAction { replaceRule(currentRule.value, f(currentRule.value), optimize = false) }
            }

            operatorButton("And") { ReportRule.Operators.And(it, defaultValue()) }
            operatorButton("Or") { ReportRule.Operators.Or(it, defaultValue()) }
            operatorButton("Not") { ReportRule.Operators.Not(it) }
        }

    private fun replaceRule(target: ReportRule, with: ReportRule, optimize: Boolean = true) {
        val newWith = if (!optimize) with else when (target) {
            is ReportRule.Operators.BinaryOperator -> when (with) {
                is ReportRule.Operators.BinaryOperator -> with.new(target.left, target.right)
                is ReportRule.Operators.UnaryOperator -> with.new(target.left)
                else -> with
            }
            is ReportRule.Operators.UnaryOperator -> when (with) {
                is ReportRule.Operators.BinaryOperator -> with.new(target.rule, with.right)
                is ReportRule.Operators.UnaryOperator -> with.new(target.rule)
                else -> with
            }
            else -> with
        }
        modifyRules { it.replace(target, newWith) }
    }

    private fun deleteRule(target: ReportRule) {
        if (target is ReportRule.Operators.UnaryOperator) {
            replaceRule(target, target.rule)
        } else {
            modifyRules { it.delete(target) ?: defaultValue() }
        }
    }

    private fun modifyRules(f: (ReportRule) -> ReportRule) {
        reportConfig = modifier(reportConfig)(f)
    }

    class Style : Stylesheet() {
        companion object {
            val ruleButton by cssclass()
            val borderedContent by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            ruleButton {
                minWidth = 120.px
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

typealias ConfigModifier = ((ReportConfig) -> ((ReportRule) -> ReportRule) -> ReportConfig)
package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.core.ReportConfig
import com.gitlab.ykrasik.gamedex.core.ReportRule
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.theme.platformComboBox
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
 * Date: 18/06/2017
 * Time: 14:34
 */
class ReportRulesFragment(
    private val modifier: ConfigModifier,
    reportConfigProperty: ObjectProperty<ReportConfig>,
    rootRule: ReportRule
) : Fragment() {

    private var reportConfig by reportConfigProperty
    private var indent = 0

    override val root = vbox {
        render(rootRule)
    }

    @Suppress("UNCHECKED_CAST")
    private fun EventTarget.render(rule: ReportRule) {
        when (rule) {
            is ReportRule.Operators.BinaryOperator -> renderOperator(rule) {
                render(rule.left)
                render(rule.right)
            }
            is ReportRule.Operators.UnaryOperator -> renderOperator(rule) {
                render(rule.rule)
            }
            is ReportRule.Rules.Rule -> renderBasicRule(rule, ReportRule.Rules.all) { ruleProperty ->
                when (ruleProperty.value) {
                    is ReportRule.Rules.PlatformRule -> renderPlatformRule(ruleProperty as ObjectProperty<ReportRule.Rules.PlatformRule>)
                    is ReportRule.Rules.CriticScore -> renderCriticScoreRule(ruleProperty as ObjectProperty<ReportRule.Rules.CriticScore>)
                    is ReportRule.Rules.UserScore -> renderUserScoreRule(ruleProperty as ObjectProperty<ReportRule.Rules.UserScore>)
                    else -> { /* Nothing to render */ }
                }
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
                                            op: (HBox.(SimpleObjectProperty<ReportRule>) -> Unit)? = null) = hbox(spacing = 5.0) {
        addClass(CommonStyle.fillAvailableWidth)
        alignment = Pos.CENTER_LEFT

        region { minWidth = indent * 10.0 }

        val ruleProperty = rule.toProperty()

        val possibleRules = all.keys.toList().observable()
        val ruleNameProperty = SimpleStringProperty(ruleProperty.value.name)

        popoverComboMenu(
            possibleItems = possibleRules,
            selectedItemProperty = ruleNameProperty,
            styleClass = Style.ruleButton,
            text = { it }
        )

        ruleNameProperty.onChange { ruleProperty.value = all[it]!!() }

        ruleProperty.addListener { _, oldValue, newValue -> replaceRule(oldValue, newValue) }

        op?.invoke(this, ruleProperty)

        spacer()

        operatorSelection(ruleProperty)
        jfxButton(graphic = Theme.Icon.delete()) { setOnAction { deleteRule(ruleProperty.value) } }
    }

    private fun HBox.renderPlatformRule(rule: ObjectProperty<ReportRule.Rules.PlatformRule>) {
        val platform = rule.mapBidirectional({ it!!.platform }, { ReportRule.Rules.PlatformRule(it!!) })
        platformComboBox(platform)
    }

    private fun HBox.renderCriticScoreRule(rule: ObjectProperty<ReportRule.Rules.CriticScore>) =
        renderScoreRule(rule, "Critic Score") { target, isGt -> ReportRule.Rules.CriticScore(target, isGt) }

    private fun HBox.renderUserScoreRule(rule: ObjectProperty<ReportRule.Rules.UserScore>) =
        renderScoreRule(rule, "User Score") { target, isGt -> ReportRule.Rules.UserScore(target, isGt) }

    private fun <T : ReportRule.Rules.TargetScoreRule> HBox.renderScoreRule(rule: ObjectProperty<T>,
                                                                            name: String,
                                                                            factory: (Double, Boolean) -> T) = hbox {
        alignment = Pos.CENTER_LEFT
        val targetValue = rule.mapBidirectional({ it!!.target }, { factory(it!!, rule.value.greaterThan) })
        adjustableTextField(targetValue, name, min = 0.0, max = 100.0)

        val isGt = rule.mapBidirectional(
            { it!!.greaterThan }, { factory(rule.value.target, it!!) }
        )
        jfxToggleButton {
            selectedProperty().bindBidirectional(isGt)
            textProperty().bind(isGt.map { if (it!!) ">=" else "<=" })
        }
    }

    private fun HBox.operatorSelection(currentRule: SimpleObjectProperty<ReportRule>) =
        buttonWithPopover(graphic = Theme.Icon.plus(), styleClass = null) {
            fun operatorButton(name: String, f: (ReportRule) -> ReportRule) = jfxButton(name) {
                addClass(CommonStyle.fillAvailableWidth)
                setOnAction { replaceRule(currentRule.value, f(currentRule.value), optimize = false) }
            }

            operatorButton("And") { ReportRule.Operators.And(it, ReportRule.Rules.True()) }
            operatorButton("Or") { ReportRule.Operators.Or(it, ReportRule.Rules.True()) }
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
            modifyRules { it.delete(target) ?: ReportRule.Rules.True() }
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
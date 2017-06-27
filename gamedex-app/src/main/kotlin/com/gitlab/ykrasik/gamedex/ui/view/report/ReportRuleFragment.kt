package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.core.ReportConfig
import com.gitlab.ykrasik.gamedex.core.ReportRule
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
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
 * Date: 25/06/2017
 * Time: 15:12
 */
class ReportRuleFragment(reportConfigProperty: ObjectProperty<ReportConfig>) : Fragment() {
    private val providerRepository: GameProviderRepository by di()

    private val gameController: GameController by di()
    private val libraryController: LibraryController by di()

    private var reportConfig by reportConfigProperty

    private val operators = mapOf(
        "And" to { ReportRule.Operators.And(ReportRule.Rules.True(), ReportRule.Rules.True()) },
        "Or" to { ReportRule.Operators.Or(ReportRule.Rules.True(), ReportRule.Rules.True()) },
        "Not" to { ReportRule.Operators.Not(ReportRule.Rules.True()) }
    )

    // TODO: I failed at decoupling the rule display name from the actual rule name
    private val rules = mapOf(
        "Critic Score" to { ReportRule.Rules.CriticScore(60.0, greaterThan = false) },
        "Has Critic Score" to { ReportRule.Rules.HasCriticScore() },

        "User Score" to { ReportRule.Rules.UserScore(60.0, greaterThan = false) },
        "Has User Score" to { ReportRule.Rules.HasUserScore() },

        "Min Score" to { ReportRule.Rules.MinScore(60.0, greaterThan = false) },
        "Has Min Score" to { ReportRule.Rules.HasMinScore() },

        "Avg Score" to { ReportRule.Rules.AvgScore(60.0, greaterThan = false) },
        "Has Avg Score" to { ReportRule.Rules.HasAvgScore() },

        "Has Platform" to { ReportRule.Rules.HasPlatform(Platform.pc) },
        "Has Provider" to { ReportRule.Rules.HasProvider(providerRepository.providers.first().id) },
        "Has Library" to { ReportRule.Rules.HasLibrary(Platform.pc, libraryController.realLibraries.firstOrNull()?.name ?: "") },
        "Has Tag" to { ReportRule.Rules.HasTag(gameController.tags.firstOrNull() ?: "") },

        "Has Duplications" to { ReportRule.Rules.Duplications() },
        "Name-Folder Diff" to { ReportRule.Rules.NameDiff() }
    )

    private var indent = 0

    override val root = vbox {
        render(reportConfig.rules)
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
            is ReportRule.Rules.Rule -> renderBasicRule(rule, rules) {
                val ruleProperty = when (rule) {
                    is ReportRule.Rules.HasPlatform -> renderPlatformRule(rule)
                    is ReportRule.Rules.HasProvider -> renderProviderRule(rule)
                    is ReportRule.Rules.HasLibrary -> renderLibraryRule(rule)
                    is ReportRule.Rules.HasTag -> renderTagRule(rule)
                    is ReportRule.Rules.CriticScore -> renderScoreRule(rule, "Critic Score", ReportRule.Rules::CriticScore)
                    is ReportRule.Rules.UserScore -> renderScoreRule(rule, "User Score", ReportRule.Rules::UserScore)
                    is ReportRule.Rules.MinScore -> renderScoreRule(rule, "Min Score", ReportRule.Rules::MinScore)
                    is ReportRule.Rules.AvgScore -> renderScoreRule(rule, "Avg Score", ReportRule.Rules::AvgScore)
                    else -> rule.toProperty()
                }
                ruleProperty.onChange { replaceRule(rule, it!!) }
            }
        }
    }

    private fun EventTarget.renderOperator(operator: ReportRule.Operators.Operator, op: VBox.() -> Unit) = vbox(spacing = 2.0) {
        addClass(Style.borderedContent)
        renderBasicRule(operator, operators)

        indent += 1
        op(this)
        indent -= 1
    }

    private fun EventTarget.renderBasicRule(rule: ReportRule,
                                            all: Map<String, () -> ReportRule>,
                                            op: (HBox.() -> Unit)? = null) = hbox(spacing = 5.0) {
        addClass(CommonStyle.fillAvailableWidth)
        alignment = Pos.CENTER_LEFT

        region { minWidth = indent * 10.0 }

        val ruleProperty = rule.toProperty()

        val possibleRules = all.keys.toList().observable()
        val ruleNameProperty = SimpleStringProperty(ruleProperty.value.name)

        popoverComboMenu(
            possibleItems = possibleRules,
            selectedItemProperty = ruleNameProperty,
            styleClass = ReportConfigFragment.Style.ruleButton,
            text = { it },
            menuOp = {      // TODO: Dirty solution!
                when (it) {
                    "Has Critic Score", "Has User Score", "Has Min Score", "Has Avg Score", "Has Tag" -> separator()
                    else -> {}
                }
            }
        )

        ruleNameProperty.onChange { ruleProperty.value = all[it]!!() }

        ruleProperty.addListener { _, oldValue, newValue -> replaceRule(oldValue, newValue) }

        op?.invoke(this)

        spacer()

        operatorSelection(ruleProperty)
        jfxButton(graphic = Theme.Icon.delete()) { setOnAction { deleteRule(ruleProperty.value) } }
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
        reportConfig = reportConfig.withRules(f)
    }

    private fun HBox.renderPlatformRule(rule: ReportRule.Rules.HasPlatform): ObjectProperty<ReportRule.Rules.HasPlatform> {
        val ruleProperty = rule.toProperty()
        val platform = ruleProperty.mapBidirectional({ it!!.platform }, { ReportRule.Rules.HasPlatform(it!!) })
        platformComboBox(platform)
        return ruleProperty
    }

    private fun HBox.renderProviderRule(rule: ReportRule.Rules.HasProvider): ObjectProperty<ReportRule.Rules.HasProvider> {
        val ruleProperty = rule.toProperty()
        val provider = ruleProperty.mapBidirectional({ it!!.providerId }, { ReportRule.Rules.HasProvider(it!!) })
        combobox(provider, providerRepository.providers.map { it.id })
        return ruleProperty
    }

    private fun HBox.renderLibraryRule(rule: ReportRule.Rules.HasLibrary): ObjectProperty<ReportRule.Rules.HasLibrary> {
        val ruleProperty = rule.toProperty()
        val library = ruleProperty.mapBidirectional(
            { libraryController.getBy(it!!.platform, it.libraryName) }, { ReportRule.Rules.HasLibrary(it!!.platform, it.name) }
        )
        popoverComboMenu(
            possibleItems = ArrayList(libraryController.realLibraries).observable(), // Avoid listener leak.
            selectedItemProperty = library,
            text = { "[${it.platform}] ${it.name}"}
        )
        return ruleProperty
    }

    private fun HBox.renderTagRule(rule: ReportRule.Rules.HasTag): ObjectProperty<ReportRule.Rules.HasTag> {
        val ruleProperty = rule.toProperty()
        val provider = ruleProperty.mapBidirectional({ it!!.tag }, { ReportRule.Rules.HasTag(it!!) })
        combobox(provider, gameController.tags)
        return ruleProperty
    }

    private fun HBox.renderScoreRule(rule: ReportRule.Rules.TargetScoreRule,
                                     name: String,
                                     factory: (Double, Boolean) -> ReportRule.Rules.TargetScoreRule): ObjectProperty<ReportRule.Rules.TargetScoreRule> {
        val ruleProperty = rule.toProperty()
        hbox {
            alignment = Pos.CENTER_LEFT
            val targetValue = ruleProperty.mapBidirectional({ it!!.target }, { factory(it!!, rule.greaterThan) })
            adjustableTextField(targetValue, name, min = 0.0, max = 100.0)

            val isGt = ruleProperty.mapBidirectional(
                { it!!.greaterThan }, { factory(rule.target, it!!) }
            )
            jfxToggleButton {
                selectedProperty().bindBidirectional(isGt)
                textProperty().bind(isGt.map { if (it!!) ">=" else "<=" })
            }
        }
        return ruleProperty
    }

    class Style : Stylesheet() {
        companion object {
            val borderedContent by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            borderedContent {
                contentDisplay = ContentDisplay.TOP
                padding = box(2.px)
                borderColor = multi(box(Color.BLACK))
                borderWidth = multi(box(0.5.px))
            }
        }
    }
}
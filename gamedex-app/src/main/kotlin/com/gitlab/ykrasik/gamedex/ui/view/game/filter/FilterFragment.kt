package com.gitlab.ykrasik.gamedex.ui.view.game.filter

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.core.Filter
import com.gitlab.ykrasik.gamedex.core.Filter.Companion.name
import com.gitlab.ykrasik.gamedex.core.FilterSet
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.theme.platformComboBox
import com.gitlab.ykrasik.gamedex.ui.widgets.adjustableTextField
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
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
 * Date: 27/01/2018
 * Time: 13:07
 */
class FilterFragment(
    private val filter: ObjectProperty<Filter>,
    private val filterSet: FilterSet
) : Fragment() {
    private val gameController: GameController by di()
    private val libraryController: LibraryController by di()
    private val providerRepository: GameProviderRepository by di()

    private var indent = 0

    val isValid = SimpleBooleanProperty(true)

    override val root = vbox {
        render(filter.value, Filter.`true`)
    }

    private fun EventTarget.render(rule: Filter, parentRule: Filter) {
        when (rule) {
            is Filter.BinaryOperator -> renderOperator(rule, parentRule) {
                render(rule.left, rule)
                render(rule.right, rule)
            }
            is Filter.UnaryOperator -> render(rule.delegate, rule)
            is Filter.Rule -> renderBasicRule(rule, parentRule, filterSet.rules) {
                val ruleProperty = when (rule) {
                    is Filter.Platform -> renderPlatformRule(rule)
                    is Filter.Library -> renderLibraryRule(rule)
                    is Filter.Genre -> renderGenreRule(rule)
                    is Filter.Tag -> renderTagRule(rule)
                    is Filter.Provider -> renderProviderRule(rule)
                    is Filter.CriticScore -> renderScoreRule(rule, Filter::CriticScore)
                    is Filter.UserScore -> renderScoreRule(rule, Filter::UserScore)
                    is Filter.AvgScore -> renderScoreRule(rule, Filter::AvgScore)
                    is Filter.FileSize -> renderFileSizeRule(rule)
                    else -> rule.toProperty()
                }
                ruleProperty.onChange { replaceRule(rule, it!!) }
            }
        }
    }

    private fun EventTarget.renderOperator(operator: Filter.Operator, parentRule: Filter, op: VBox.() -> Unit) = vbox(spacing = 2.0) {
        addClass(Style.borderedContent)
        renderBasicRule(operator, parentRule, filterSet.operators)

        indent += 1
        op(this)
        indent -= 1
    }

    private fun EventTarget.renderBasicRule(rule: Filter,
                                            parentRule: Filter,
                                            possible: List<String>,
                                            op: (HBox.() -> Unit)? = null) = hbox(spacing = 5.0) {
        addClass(CommonStyle.fillAvailableWidth)
        alignment = Pos.CENTER_LEFT

        region { minWidth = indent * 10.0 }

        val ruleNameProperty = SimpleStringProperty(rule.name)

        popoverComboMenu(
            possibleItems = possible,
            selectedItemProperty = ruleNameProperty,
            styleClass = Style.ruleButton,
            text = { it },
            menuOp = {
                when (it) {
                    Filter.Platform::class.name, Filter.AvgScore::class.name, Filter.Provider::class.name -> separator()
                }
            }
        )

        ruleNameProperty.onChange { name ->
            val newRule = filterSet.new(name!!)
            replaceRule(rule, newRule)
        }

        op?.invoke(this)

        spacer()

        renderAdditionalButtons(rule, parentRule)
    }

    private fun HBox.renderAdditionalButtons(currentRule: Filter, parentRule: Filter) {
        val negated = parentRule is Filter.Not
        val target = if (negated) parentRule else currentRule
        fun replaceRule(f: (Filter) -> Filter) = replaceRule(target, f(target))
        buttonWithPopover(graphic = Theme.Icon.plus(), styleClass = null) {
            fun operatorButton(name: String, f: (Filter) -> Filter) = jfxButton(name) {
                addClass(CommonStyle.fillAvailableWidth)
                setOnAction { replaceRule(f) }
            }

            operatorButton("And") { Filter.And(it) }
            operatorButton("Or") { Filter.Or(it) }
        }
        jfxToggleNode(graphic = Theme.Icon.not()) {
            tooltip("Not")
            isSelected = negated
            selectedProperty().onChange { selected ->
                when {
                    selected && !negated -> replaceRule { Filter.Not(it) }
                    !selected && negated -> replaceRule(target, (target as Filter.Not).delegate)
                }
            }
        }
        jfxButton(graphic = Theme.Icon.delete()) {
            setOnAction {
                modifyFilter { it.delete(target) ?: Filter.`true` }
            }
        }
    }

    private fun replaceRule(target: Filter, with: Filter) =
        modifyFilter { it.replace(target, with) }

    private fun modifyFilter(f: (Filter) -> Filter) {
        filter.value = f(filter.value)
    }

    private fun HBox.renderPlatformRule(rule: Filter.Platform) = rule.toProperty().apply {
        val platform = mapBidirectional({ it!!.platform }, { Filter.Platform(it!!) })
        platformComboBox(platform)
    }

    private fun HBox.renderProviderRule(rule: Filter.Provider) = rule.toProperty().apply {
        val provider = mapBidirectional({ it!!.providerId }, { Filter.Provider(it!!) })
        combobox(provider, providerRepository.providers.map { it.id })
    }

    private fun HBox.renderLibraryRule(rule: Filter.Library) = rule.toProperty().apply {
        val library = mapBidirectional(
            { libraryController.getBy(it!!.platform, it.libraryName) }, { Filter.Library(it!!.platform, it.name) }
        )
        popoverComboMenu(
            possibleItems = libraryController.platformLibraries as List<Library>,
            selectedItemProperty = library,
            text = { it.name }
        )
    }

    private fun HBox.renderGenreRule(rule: Filter.Genre) = rule.toProperty().apply {
        val genre = mapBidirectional({ it!!.genre }, { Filter.Genre(it!!) })
        popoverComboMenu(
            possibleItems = gameController.genres as List<String>,
            selectedItemProperty = genre,
            text = { it }
        )
    }

    private fun HBox.renderTagRule(rule: Filter.Tag) = rule.toProperty().apply {
        val tag = mapBidirectional({ it!!.tag }, { Filter.Tag(it!!) })
        combobox(tag, gameController.tags)
    }

    private fun HBox.renderScoreRule(rule: Filter.ScoreRule, factory: (Double?) -> Filter.ScoreRule) = rule.toProperty().apply {
        hbox(spacing = 5.0) {
            alignment = Pos.CENTER_LEFT

            val target = mapBidirectional({ it!!.target }, { factory(it) })
            jfxButton {
                text = if (rule.target == null) "is null" else ">="
                setOnAction {
                    target.value = if (rule.target == null) 60.0 else null
                }
            }
            if (rule.target != null) {
                @Suppress("UNCHECKED_CAST")
                adjustableTextField(target as Property<Double>, rule.name, min = 0.0, max = 100.0, withButtons = false)
            }
        }
    }

    private fun HBox.renderFileSizeRule(rule: Filter.FileSize) = rule.toProperty().apply {
        val fragment = FileSizeRuleFragment(this)
        children += fragment.root
        isValid.bind(fragment.isValid)  // TODO: Handle more then 1 possibly invalid rules.
    }

    class Style : Stylesheet() {
        companion object {
            val filterItem by cssclass()
            val borderedContent by cssclass()
            val ruleButton by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            filterItem {
                minWidth = 160.px
            }

            borderedContent {
                contentDisplay = ContentDisplay.TOP
                padding = box(2.px)
                borderColor = multi(box(Color.BLACK))
                borderWidth = multi(box(0.5.px))
            }

            ruleButton {
                minWidth = 120.px
                alignment = Pos.CENTER_LEFT
            }
        }
    }
}
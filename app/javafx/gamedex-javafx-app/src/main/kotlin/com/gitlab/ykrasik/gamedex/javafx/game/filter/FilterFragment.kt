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

package com.gitlab.ykrasik.gamedex.javafx.game.filter

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.core.FilterSet
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.api.util.behaviorSubject
import com.gitlab.ykrasik.gamedex.core.api.util.value_
import com.gitlab.ykrasik.gamedex.core.game.Filter
import com.gitlab.ykrasik.gamedex.core.game.Filter.Companion.name
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.adjustableTextField
import com.gitlab.ykrasik.gamedex.javafx.game.GameController
import com.gitlab.ykrasik.gamedex.javafx.library.LibraryController
import com.gitlab.ykrasik.gamedex.util.toJava
import com.gitlab.ykrasik.gamedex.util.toJoda
import io.reactivex.Observable
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
// TODO: Create a 'GlobalStorage' class that will contain all games, libraries, genres, tags etc?
class FilterFragment(private val filterObservable: Observable<Filter>, private val filterSet: FilterSet) : Fragment() {
    private val gameController: GameController by di()
    private val libraryController: LibraryController by di()
    private val gameProviderService: GameProviderService by di()

    // This is slightly hacky.
    private lateinit var rootFilter: Filter
    private var indent = 0

    val isValid = SimpleBooleanProperty(true)

    private val newFilterSubject = behaviorSubject<Filter>()
    val newFilterObservable: Observable<Filter> = newFilterSubject

    override val root = vbox {
        filterObservable.subscribe { filter ->
            rootFilter = filter
            replaceChildren {
                render(filter, Filter.`true`)
            }
        }
    }

    private fun EventTarget.render(filter: Filter, parentFilter: Filter) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (filter) {
            is Filter.BinaryOperator -> renderOperator(filter, parentFilter) {
                render(filter.left, filter)
                render(filter.right, filter)
            }
            is Filter.UnaryOperator -> render(filter.delegate, filter)
            is Filter.Rule -> renderBasicRule(filter, parentFilter, filterSet.rules) {
                // TODO: Use observables here instead of properties?
                val ruleProperty = when (filter) {
                    is Filter.Platform -> renderPlatformFilter(filter)
                    is Filter.Library -> renderLibraryFilter(filter)
                    is Filter.Genre -> renderGenreFilter(filter)
                    is Filter.Tag -> renderTagFilter(filter)
                    is Filter.ReleaseDate -> renderReleaseDateFilter(filter)
                    is Filter.Provider -> renderProviderFilter(filter)
                    is Filter.CriticScore -> renderScoreFilter(filter, Filter::CriticScore)
                    is Filter.UserScore -> renderScoreFilter(filter, Filter::UserScore)
                    is Filter.AvgScore -> renderScoreFilter(filter, Filter::AvgScore)
                    is Filter.FileSize -> renderFileSizeFilter(filter)
                    else -> filter.toProperty()
                }
                ruleProperty.onChange { replaceFilter(filter, it!!) }
            }
        }
    }

    private fun EventTarget.renderOperator(operator: Filter.Operator, parentFilter: Filter, op: VBox.() -> Unit) = vbox(spacing = 2.0) {
        addClass(Style.borderedContent)
        renderBasicRule(operator, parentFilter, filterSet.operators)

        indent += 1
        op(this)
        indent -= 1
    }

    private fun EventTarget.renderBasicRule(filter: Filter,
                                            parentFilter: Filter,
                                            possible: List<String>,
                                            op: (HBox.() -> Unit)? = null) = hbox(spacing = 5.0) {
        addClass(CommonStyle.fillAvailableWidth)
        alignment = Pos.CENTER_LEFT

        region { minWidth = indent * 10.0 }

        val ruleNameProperty = SimpleStringProperty(filter.name)

        popoverComboMenu(
            possibleItems = possible,
            selectedItemProperty = ruleNameProperty,
            styleClass = Style.ruleButton,
            menuOp = {
                when (it) {
                    Filter.Platform::class.name, Filter.AvgScore::class.name, Filter.Provider::class.name -> separator()
                }
            }
        )

        // TODO: Put this logic in a presenter
        ruleNameProperty.onChange { name ->
            val newRule = filterSet.new(name!!).let { newRule ->
                if (filter is Filter.BinaryOperator && newRule is Filter.BinaryOperator) {
                    newRule.new(filter.left, filter.right)
                } else {
                    newRule
                }
            }
            replaceFilter(filter, newRule)
        }

        op?.invoke(this)

        spacer()

        renderAdditionalButtons(filter, parentFilter)
    }

    private fun HBox.renderAdditionalButtons(currentFilter: Filter, parentFilter: Filter) {
        val negated = parentFilter is Filter.Not
        val target = if (negated) parentFilter else currentFilter
        fun modifyFilter(f: (Filter) -> Filter) = replaceFilter(target, f(target))
        buttonWithPopover(graphic = Theme.Icon.plus(), styleClass = null) {
            fun operatorButton(name: String, f: (Filter) -> Filter) = jfxButton(name) {
                addClass(CommonStyle.fillAvailableWidth)
                setOnAction { modifyFilter(f) }
            }

            operatorButton("And") { Filter.And(it) }
            operatorButton("Or") { Filter.Or(it) }
        }
        jfxToggleNode(graphic = Theme.Icon.not()) {
            tooltip("Not")
            isSelected = negated
            selectedProperty().onChange { selected ->
                when {
                    selected && !negated -> modifyFilter { Filter.Not(it) }
                    !selected && negated -> replaceFilter(target, (target as Filter.Not).delegate)
                }
            }
        }
        jfxButton(graphic = Theme.Icon.delete()) {
            setOnAction {
                replaceFilter(target, with = null)
            }
        }
    }

    // TODO: Put this logic in a presenter
    private fun replaceFilter(target: Filter, with: Filter?) {
        newFilterSubject.value_ = if (with == null) {
            rootFilter.delete(target) ?: Filter.`true`
        } else {
            rootFilter.replace(target, with)
        }
    }

    private fun HBox.renderPlatformFilter(rule: Filter.Platform) = rule.toProperty().apply {
        val platform = mapBidirectional(Filter.Platform::platform, Filter::Platform)
        platformComboBox(platform)
    }

    private fun HBox.renderProviderFilter(rule: Filter.Provider) = rule.toProperty().apply {
        val provider = mapBidirectional(Filter.Provider::providerId, Filter::Provider)
        combobox(provider, gameProviderService.allProviders.map { it.id })
    }

    private fun HBox.renderLibraryFilter(rule: Filter.Library) = rule.toProperty().apply {
        val library = mapBidirectional(
            { libraryController.getBy(platform, libraryName) }, { Filter.Library(platform, name) }
        )
        popoverComboMenu(
            possibleItems = libraryController.platformLibraries as List<Library>,
            selectedItemProperty = library,
            text = { it.name },
            graphic = { it.platform.toLogo() }
        )
    }

    private fun HBox.renderGenreFilter(rule: Filter.Genre) = rule.toProperty().apply {
        val genre = mapBidirectional(Filter.Genre::genre, Filter::Genre)
        popoverComboMenu(
            possibleItems = gameController.genres as List<String>,
            selectedItemProperty = genre
        )
    }

    private fun HBox.renderTagFilter(rule: Filter.Tag) = rule.toProperty().apply {
        val tag = mapBidirectional(Filter.Tag::tag, Filter::Tag)
        popoverComboMenu(
            possibleItems = gameController.tags as List<String>,
            selectedItemProperty = tag
        )
    }

    private fun HBox.renderReleaseDateFilter(rule: Filter.ReleaseDate) = rule.toProperty().apply {
        val releaseDate = mapBidirectional({ releaseDate.toJava() }, { Filter.ReleaseDate(toJoda()) })
        datepicker {
            valueProperty().bindBidirectional(releaseDate)
        }
    }

    private fun HBox.renderScoreFilter(rule: Filter.ScoreRule, factory: (Double) -> Filter.ScoreRule) = rule.toProperty().apply {
        hbox(spacing = 5.0) {
            alignment = Pos.CENTER_LEFT

            val target = mapBidirectional(Filter.ScoreRule::target, factory)
            jfxButton {
                text = if (rule.target == Filter.ScoreRule.NoScore) "is null" else ">="
                setOnAction {
                    target.value = if (rule.target == Filter.ScoreRule.NoScore) 60.0 else Filter.ScoreRule.NoScore
                }
            }
            if (rule.target != Filter.ScoreRule.NoScore) {
                @Suppress("UNCHECKED_CAST")
                adjustableTextField(target as Property<Double>, rule.name, min = 0.0, max = 100.0, withButtons = false)
            }
        }
    }

    private fun HBox.renderFileSizeFilter(filter: Filter.FileSize) = filter.toProperty().apply {
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
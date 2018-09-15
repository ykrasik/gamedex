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
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter.Companion.name
import com.gitlab.ykrasik.gamedex.app.api.game.GameFilterView
import com.gitlab.ykrasik.gamedex.app.api.game.MenuGameFilterView
import com.gitlab.ykrasik.gamedex.app.api.game.ReportGameFilterView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.adjustableTextField
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.toJava
import com.gitlab.ykrasik.gamedex.util.toJoda
import javafx.beans.property.Property
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import kotlinx.coroutines.experimental.channels.Channel
import tornadofx.*
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 27/01/2018
 * Time: 13:07
 */
abstract class BaseJavaFxGameFilterView : PresentableView(), GameFilterView {
    override val possibleGenres = mutableListOf<String>()
    override val possibleTags = mutableListOf<String>()
    override val possibleLibraries = mutableListOf<Library>()
    override val possibleProviderIds = mutableListOf<ProviderId>()
    override val possibleRules = mutableListOf<KClass<out Filter.Rule>>().observable()

    private val _filterProperty = SimpleObjectProperty<Filter>(Filter.`true`)
    val filterProperty: ReadOnlyObjectProperty<Filter> = _filterProperty
    override var filter by _filterProperty

    override val wrapInAndActions = channel<Filter>()
    override val wrapInOrActions = channel<Filter>()
    override val wrapInNotActions = channel<Filter>()
    override val unwrapNotActions = channel<Filter.Not>()
    override val clearFilterActions = channel<Unit>()
    override val updateFilterActions = channel<Pair<Filter.Rule, Filter.Rule>>()
    override val replaceFilterActions = channel<Pair<Filter, KClass<out Filter>>>()
    override val deleteFilterActions = channel<Filter>()

    private var indent = 0

    val isValid = SimpleBooleanProperty(true)

    override val root = vbox {
        fun rerender() = replaceChildren {
            render(_filterProperty.value, Filter.`true`)
        }
        _filterProperty.onChange { rerender() }
        possibleRules.onChange { rerender() }
    }

    init {
        viewRegistry.register(this)
    }

    private fun EventTarget.render(filter: Filter, parentFilter: Filter) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (filter) {
            is Filter.BinaryOperator -> renderOperator(filter, parentFilter) {
                render(filter.left, filter)
                render(filter.right, filter)
            }
            is Filter.UnaryOperator -> render(filter.target, filter)
            is Filter.Rule -> renderBasicRule(filter, parentFilter, possibleRules) {
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
                ruleProperty.eventOnChange(updateFilterActions) { filter to it }
            }
            else -> kotlin.error("Unknown filter: $filter")
        }
    }

    private fun EventTarget.renderOperator(operator: Filter.Operator, parentFilter: Filter, op: VBox.() -> Unit) = vbox(spacing = 2.0) {
        addClass(Style.borderedContent)
        renderBasicRule(operator, parentFilter, possible = listOf(Filter.And::class, Filter.Or::class))

        indent += 1
        op(this)
        indent -= 1
    }

    private fun EventTarget.renderBasicRule(filter: Filter,
                                            parentFilter: Filter,
                                            possible: List<KClass<out Filter>>,
                                            op: (HBox.() -> Unit)? = null) = hbox(spacing = 5.0) {
        useMaxWidth = true
        alignment = Pos.CENTER_LEFT

        region { minWidth = indent * 10.0 }

        val ruleProperty = SimpleObjectProperty(filter::class).eventOnChange(replaceFilterActions) { filter to it }

        popoverComboMenu(
            possibleItems = possible,
            selectedItemProperty = ruleProperty,
            styleClass = Style.ruleButton,
            text = { it.name },
            menuOp = {
                when (it) {
                    Filter.Platform::class, Filter.AvgScore::class, Filter.Provider::class -> separator()
                }
            }
        )

        op?.invoke(this)

        spacer()

        renderAdditionalButtons(filter, parentFilter)
    }

    private fun HBox.renderAdditionalButtons(currentFilter: Filter, parentFilter: Filter) {
        val negated = parentFilter is Filter.Not
        val target = if (negated) parentFilter else currentFilter
        buttonWithPopover(graphic = Theme.Icon.plus(), styleClass = null) {
            fun operatorButton(name: String, channel: Channel<Filter>) = jfxButton(name) {
                useMaxWidth = true
                eventOnAction(channel) { target }
            }

            operatorButton("And", wrapInAndActions)
            operatorButton("Or", wrapInOrActions)
        }
        jfxToggleNode(graphic = Theme.Icon.not()) {
            tooltip("Not")
            isSelected = negated
            selectedProperty().onChange { selected ->
                when {
                    selected && !negated -> wrapInNotActions.event(target)
                    !selected && negated -> unwrapNotActions.event(target as Filter.Not)
                }
            }
        }
        jfxButton(graphic = Theme.Icon.delete()) { eventOnAction(deleteFilterActions) { target } }
    }

    private fun HBox.renderPlatformFilter(rule: Filter.Platform) = rule.toProperty().apply {
        val platform = mapBidirectional(Filter.Platform::platform, Filter::Platform)
        platformComboBox(platform)
    }

    private fun HBox.renderProviderFilter(rule: Filter.Provider) = rule.toProperty().apply {
        val provider = mapBidirectional(Filter.Provider::providerId, Filter::Provider)
        combobox(provider, possibleProviderIds)
    }

    private fun HBox.renderLibraryFilter(rule: Filter.Library) = rule.toProperty().apply {
        val library = mapBidirectional({ possibleLibraries.find { it.id == id }!! }, { Filter.Library(id) })
        popoverComboMenu(
            possibleItems = possibleLibraries,
            selectedItemProperty = library,
            text = { it.name },
            graphic = { it.platform.toLogo() }
        )
    }

    private fun HBox.renderGenreFilter(rule: Filter.Genre) = rule.toProperty().apply {
        val genre = mapBidirectional(Filter.Genre::genre, Filter::Genre)
        popoverComboMenu(
            possibleItems = possibleGenres,
            selectedItemProperty = genre
        )
    }

    private fun HBox.renderTagFilter(rule: Filter.Tag) = rule.toProperty().apply {
        val tag = mapBidirectional(Filter.Tag::tag, Filter::Tag)
        popoverComboMenu(
            possibleItems = possibleTags,
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
        isValid.bind(fragment.isValid)  // TODO: Handle more than 1 possibly invalid rules.
    }

    class Style : Stylesheet() {
        companion object {
            val borderedContent by cssclass()
            val ruleButton by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
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

class JavaFxMenuGameFilterView : BaseJavaFxGameFilterView(), MenuGameFilterView
class JavaFxReportGameFilterView : BaseJavaFxGameFilterView(), ReportGameFilterView
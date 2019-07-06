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

package com.gitlab.ykrasik.gamedex.app.javafx.filter

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.TagId
import com.gitlab.ykrasik.gamedex.app.api.filter.*
import com.gitlab.ykrasik.gamedex.app.api.util.MultiChannel
import com.gitlab.ykrasik.gamedex.app.api.util.UserMutableState
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.Extractor
import com.gitlab.ykrasik.gamedex.util.FileSize
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.caseInsensitiveStringComparator
import javafx.beans.property.Property
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import kotlinx.coroutines.Job
import org.controlsfx.control.PopOver
import org.joda.time.DurationFieldType
import org.joda.time.LocalDate
import org.joda.time.Period
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*
import java.util.*
import java.util.function.Predicate
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 27/01/2018
 * Time: 13:07
 */
class JavaFxFilterView(
    allowSaveLoad: Boolean = true,
    private val readOnly: Boolean = false,
    private val preProcessHeader: HBox.() -> Unit = {}
) : PresentableView(),
    FilterView,
    ViewWithFilters,
    ViewCanAddOrEditFilter,
    ViewCanDeleteFilter {

    override val filter = state(Filter.Null)
    override val filterIsValid = state(IsValid.valid)
    override val setFilterActions = channel<Filter>()

    override val availableFilters = settableList<KClass<out Filter.Rule>>()

    override val availableLibraries = settableList<Library>()
    private val availableLibrariesSortedFiltered = availableLibraries.sortedFiltered(caseInsensitiveStringComparator(Library::name))

    override val availableGenres = settableList<String>()
    private val availableGenresSortedFiltered = availableGenres.sortedFiltered(caseInsensitiveStringComparator)

    override val availableTags = settableList<TagId>()
    private val availableTagsSortedFiltered = availableTags.sortedFiltered(caseInsensitiveStringComparator)

    override val availableFilterTags = settableList<TagId>()
    private val availableFilterTagsSortedFiltered = availableFilterTags.sortedFiltered(caseInsensitiveStringComparator)

    override val availableProviderIds = settableList<ProviderId>()
    private val availableProviderIdsSortedFiltered = availableProviderIds.sortedFiltered(caseInsensitiveStringComparator)

    override val savedFilters = settableList<NamedFilter>()
    private val savedFiltersFilteredSorted = savedFilters.sortedFiltered(caseInsensitiveStringComparator(NamedFilter::id))

    override val wrapInAndActions = channel<Filter>()
    override val wrapInOrActions = channel<Filter>()
    override val wrapInNotActions = channel<Filter>()
    override val unwrapNotActions = channel<Filter.Not>()
    override val clearFilterActions = channel<Unit>()
    override val updateFilterActions = channel<Pair<Filter.Rule, Filter.Rule>>()
    override val replaceFilterActions = channel<Pair<Filter, KClass<out Filter>>>()
    override val deleteFilterActions = channel<Filter>()

    override val addOrEditFilterActions = channel<NamedFilter>()
    override val deleteNamedFilterActions = channel<NamedFilter>()

    private val savedFilterPreviewContent: JavaFxFilterView by lazy { JavaFxFilterView(allowSaveLoad = false, readOnly = true) }
    private val savedFilterPreview by lazy {
        popOver(arrowLocation = PopOver.ArrowLocation.LEFT_TOP) {
            addComponent(savedFilterPreviewContent)
        }
    }

    private val commonOps: JavaFxCommonOps by di()

    private var indent = 0

    private val prevCache: MutableMap<Filter, Pane> = IdentityHashMap()
    private val currentCache: MutableMap<Filter, Pane> = IdentityHashMap()

    val userMutableState = object : UserMutableState<Filter> {
        private var prevExternalFilter: Filter? = null

        override var value: Filter
            get() = filter.value
            set(value) {
                prevExternalFilter = value
                // We have to set the value of the filter manually before sending an event to setFilterActions,
                // because due to the async nature of channels, the code that relies on this value to have
                // been set will actually run before the code of setFilterActions had a chance to run.
                filter.value = value
                setFilterActions.offer(value)
            }

        override val changes = MultiChannel<Filter>()

        init {
            filter.property.onInvalidated { filter ->
                if (filter !== prevExternalFilter) {
                    changes.offer(filter)
                }
                prevExternalFilter = null
            }
        }
    }

    override val root = defaultVbox {
        isMouseTransparent = readOnly
        if (allowSaveLoad) {
            defaultHbox {
                preProcessHeader()
                spacer()
                buttonWithPopover("Saved Filters", Icons.files.size(22), arrowLocation = PopOver.ArrowLocation.LEFT_TOP, closeOnAction = false) {
                    setFilterActions.forEach { hide() }
                    addOrEditFilterActions.forEach { hide() }
                    deleteNamedFilterActions.forEach { hide() }

                    val searchProperty = SimpleStringProperty("")
                    searchTextField(this@JavaFxFilterView, searchProperty)
                    savedFiltersFilteredSorted.filteredItems.predicateProperty().bind(searchProperty.binding { text ->
                        Predicate<NamedFilter> { filter ->
                            text.isEmpty() || filter.id.contains(text, ignoreCase = true)
                        }
                    })

                    addButton(isToolbarButton = false) {
                        alignment = Pos.CENTER
                        useMaxWidth = true
                        tooltip("Add a new filter")
                        action(addOrEditFilterActions) { NamedFilter.anonymous(filter.value) }
                    }
                    defaultVbox {
                        savedFiltersFilteredSorted.perform { filters ->
                            replaceChildren {
                                gridpane {
                                    hgap = 5.0
                                    vgap = 3.0
                                    usePrefWidth = true
                                    filters.forEach { filter ->
                                        row {
                                            infoButton(filter.id, graphic = Icons.load.size(22)) {
                                                useMaxWidth = true
                                                gridpaneColumnConstraints { hgrow = Priority.ALWAYS }
                                                alignment = Pos.CENTER_LEFT
                                                previewFilterOnHover(filter.filter)
                                                action(setFilterActions) { filter.filter }
                                            }
                                            editButton(isToolbarButton = false) {
                                                action(addOrEditFilterActions) { filter }
                                            }
                                            deleteButton(isToolbarButton = false) {
                                                action(deleteNamedFilterActions) { filter }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.apply {
                    addClass(GameDexStyle.toolbarButton)
                    isFocusTraversable = false
                    tooltip("Saved Filters")
                }
            }
            verticalGap()
        }
        vbox {
            filter.property.addListener { _, oldValue, newValue -> reRender(newValue, prevFilter = oldValue) }
            availableFilters.onChange { reRender() }

            fun conditionalReRender(klass: KClass<out Filter>) {
                if (filter.value.hasFilter(klass)) {
                    reRender()
                }
            }
            availableLibrariesSortedFiltered.onChange { conditionalReRender(Filter.Library::class) }
            availableGenresSortedFiltered.onChange { conditionalReRender(Filter.Genre::class) }
            availableTagsSortedFiltered.onChange { conditionalReRender(Filter.Tag::class) }
            availableFilterTagsSortedFiltered.onChange { conditionalReRender(Filter.FilterTag::class) }
            availableProviderIdsSortedFiltered.onChange { conditionalReRender(Filter.Provider::class) }
        }
    }

    init {
        // This HAS to be called AFTER registering the above listeners which re-render the filters!!!
        if (!readOnly) {
            register()
        }
    }

    private fun Node.previewFilterOnHover(filter: Filter) {
        var debounceJob: Job? = null
        setOnMouseEntered {
            debounceJob?.cancel()
            debounceJob = debounce(millis = 200) {
                savedFilterPreviewContent.userMutableState.value = filter
                savedFilterPreview.show(this@previewFilterOnHover)
            }
        }
        setOnMouseExited {
            debounceJob?.cancel()
            savedFilterPreview.hide()
        }
    }

    private fun Node.reRender(filter: Filter = this@JavaFxFilterView.filter.value, prevFilter: Filter? = null): Unit = replaceChildren {
        prevCache.clear()
        prevCache.putAll(currentCache)
        currentCache.clear()
        render(filter, parentFilter = Filter.Null, prevFilter = prevFilter)
    }

    private fun EventTarget.render(filter: Filter, parentFilter: Filter, prevFilter: Filter?): Pane {
        val pane: Pane = when {
            filter === prevFilter -> prevCache.getValue(filter).also { add(it) }
            filter is Filter.Compound -> renderCompoundFilter(filter, parentFilter) {
                val prevFilters = (prevFilter as? Filter.Compound)?.targets ?: emptyList()
                filter.targets.forEachIndexed { i, targetFilter ->
                    val prevTargetFilter = prevFilters.find { it === targetFilter } ?: prevFilters.getOrNull(i)
                    render(targetFilter, parentFilter = filter, prevFilter = prevTargetFilter)
                }
            }
            filter is Filter.Modifier -> render(filter.target, parentFilter = filter, prevFilter = (prevFilter as? Filter.Modifier)?.target)
            filter is Filter.Rule -> renderBasicRule(filter, parentFilter, availableFilters) {
                when (filter) {
                    is Filter.Platform -> renderPlatformFilter(filter)
                    is Filter.Library -> renderLibraryFilter(filter)
                    is Filter.Genre -> renderGenreFilter(filter)
                    is Filter.Tag -> renderTagFilter(filter)
                    is Filter.FilterTag -> renderFilterTagFilter(filter)
                    is Filter.Provider -> renderProviderFilter(filter)
                    is Filter.CriticScore -> renderScoreFilter(filter, Filter::CriticScore)
                    is Filter.UserScore -> renderScoreFilter(filter, Filter::UserScore)
                    is Filter.AvgScore -> renderScoreFilter(filter, Filter::AvgScore)
                    is Filter.MinScore -> renderScoreFilter(filter, Filter::MinScore)
                    is Filter.MaxScore -> renderScoreFilter(filter, Filter::MaxScore)
                    is Filter.TargetReleaseDate -> renderTargetDateFilter(filter, Filter::TargetReleaseDate)
                    is Filter.PeriodReleaseDate -> renderPeriodDateFilter(filter, Filter::PeriodReleaseDate)
                    is Filter.NullReleaseDate -> renderNullFilter()
                    is Filter.TargetCreateDate -> renderTargetDateFilter(filter, Filter::TargetCreateDate)
                    is Filter.PeriodCreateDate -> renderPeriodDateFilter(filter, Filter::PeriodCreateDate)
                    is Filter.TargetUpdateDate -> renderTargetDateFilter(filter, Filter::TargetUpdateDate)
                    is Filter.PeriodUpdateDate -> renderPeriodDateFilter(filter, Filter::PeriodUpdateDate)
                    is Filter.FileSize -> renderFileSizeFilter(filter)
                    is Filter.FileName -> renderFileNameFilter(filter, prevFilter)
                    else -> filter.toProperty()
                }
            }
            else -> kotlin.error("Unknown filter: $filter")
        }
        currentCache[filter] = pane
        return pane
    }

    private fun EventTarget.renderCompoundFilter(metaFilter: Filter.Compound, parentFilter: Filter, op: VBox.() -> Unit) = vbox(spacing = 2) {
        renderBasicRule(metaFilter, parentFilter, availableFilters = listOf(Filter.And::class, Filter.Or::class))

        indent += 1
        op(this)
        indent -= 1
    }

    private fun EventTarget.renderBasicRule(
        filter: Filter,
        parentFilter: Filter,
        availableFilters: List<KClass<out Filter>>,
        op: HBox.() -> Unit = {}
    ) = defaultHbox {
        useMaxWidth = true

        gap(size = indent * 20.0)

        val descriptor = filter.descriptor
        popOverMenu(descriptor.selectedName, descriptor.selectedIcon().size(26)) {
            // TODO: As an optimization, can share this popover across all menus.
            fun EventTarget.filterButton(descriptor: FilterDisplayDescriptor) =
                jfxButton(descriptor.name, descriptor.icon().size(26), alignment = Pos.CENTER_LEFT) {
                    useMaxWidth = true
                    action { replaceFilterActions.event(filter to descriptor.filter) }
                }

            val subMenus = mutableMapOf<FilterDisplaySubMenu, PopOverMenu>()
            fun subMenu(descriptor: FilterDisplaySubMenu, f: PopOverMenu.() -> Unit) = f(subMenus.getOrPut(descriptor) {
                lateinit var menu: PopOverMenu
                popOverSubMenu(descriptor.text, descriptor.icon().size(26)) { menu = this }
                menu
            })

            var needGap = false
            availableFilters.forEach {
                if (needGap) {
                    verticalGap(size = 20)
                }

                val newDescriptor = it.descriptor
                if (newDescriptor.subMenu != null) {
                    subMenu(newDescriptor.subMenu) {
                        filterButton(newDescriptor)
                    }
                } else {
                    filterButton(newDescriptor)
                }

                needGap = newDescriptor.gap
            }
        }.apply {
            minWidth = 120.0
        }

        val negated = parentFilter is Filter.Not
        val target = if (negated) parentFilter else filter
        jfxButton(graphic = (if (negated) descriptor.negatedActionIcon() else descriptor.actionIcon()).size(26)) {
            tooltip("Reverse filter")
            isDisable = filter.isEmpty
            action {
                if (negated) {
                    unwrapNotActions.event(target as Filter.Not)
                } else {
                    wrapInNotActions.event(target)
                }
            }
        }

        op()

        if (!readOnly) {
            spacer()
            gap()
            renderAdditionalButtons(target)
        }
    }

    private fun HBox.renderAdditionalButtons(filter: Filter) {
        // TODO: Use a NodeList?
        buttonWithPopover(graphic = Icons.plus.size(28)) {
            fun operatorButton(name: String, graphic: Node, channel: MultiChannel<Filter>) = jfxButton(name, graphic, alignment = Pos.CENTER_LEFT) {
                useMaxWidth = true
                action(channel) { filter }
            }

            operatorButton("And", Icons.and, wrapInAndActions)
            operatorButton("Or", Icons.or, wrapInOrActions)
        }.apply {
            tooltip("Add filter")
        }
        deleteButton(isToolbarButton = false) {
            // Do not allow deleting an empty root
            isDisable = filter === this@JavaFxFilterView.filter.value && filter.isEmpty
            action(deleteFilterActions) { filter }
        }
    }

    private fun HBox.renderPlatformFilter(filter: Filter.Platform) {
        val platform = filter.toProperty(Filter.Platform::platform, Filter::Platform)
        platformComboBox(platform)
    }

    private fun HBox.renderLibraryFilter(filter: Filter.Library) {
        // There is a race condition when switching platforms with a library filter active -
        // The actual filter & the possible rules are updated by different presenters, which can lead
        // to situations where the possible libraries already point to platform specific libraries
        // but the filter is still the filter for the old platform, which will mean the library isn't found.
        // Currently there's no other solution but to tolerate this situation, as the presenter in charge of
        // the platform filter will call us just a bit later and fix it.
        val library = filter.toProperty(
            { availableLibrariesSortedFiltered.find { it.id == id } ?: Library.Null },
            { Filter.Library(it.id) }
        )
        popoverComboMenu(
            possibleItems = availableLibrariesSortedFiltered,
            selectedItemProperty = library,
            text = { it.name },
            graphic = { it.platformOrNull?.logo }
        )
    }

    private fun HBox.renderGenreFilter(filter: Filter.Genre) {
        val genre = filter.toProperty(Filter.Genre::genre, Filter::Genre)
        popoverComboMenu(
            possibleItems = availableGenresSortedFiltered,
            selectedItemProperty = genre
        )
    }

    private fun HBox.renderTagFilter(filter: Filter.Tag) {
        val tag = filter.toProperty(Filter.Tag::tag, Filter::Tag)
        popoverComboMenu(
            possibleItems = availableTagsSortedFiltered,
            selectedItemProperty = tag
        )
    }

    private fun HBox.renderFilterTagFilter(filter: Filter.FilterTag) {
        val tag = filter.toProperty(Filter.FilterTag::tag, Filter::FilterTag)
        popoverComboMenu(
            possibleItems = availableFilterTagsSortedFiltered,
            selectedItemProperty = tag
        )
    }

    private fun HBox.renderProviderFilter(filter: Filter.Provider) {
        val provider = filter.toProperty(Filter.Provider::providerId, Filter::Provider)
        popoverComboMenu(
            possibleItems = availableProviderIdsSortedFiltered,
            selectedItemProperty = provider,
            graphic = { commonOps.providerLogo(it).toImageView(height = 28) },
            itemOp = { alignment = Pos.CENTER }
        )
    }

    private fun HBox.renderScoreFilter(filter: Filter.TargetScore, factory: (Double) -> Filter.TargetScore) {
        val target = filter.toProperty(Filter.TargetScore::score, factory)
        plusMinusSlider(target, min = 0, max = 100)
    }

    private fun HBox.renderTargetDateFilter(filter: Filter.TargetDate, factory: (LocalDate) -> Filter.TargetDate) {
        tooltip("Is after the given date")
        val date = filter.toProperty(Filter.TargetDate::date, factory)
        jfxDatePicker(date)
    }

    private fun HBox.renderPeriodDateFilter(filter: Filter.PeriodDate, factory: (Period) -> Filter.PeriodDate) {
        tooltip("Is within a duration ago from now")
        val initialPeriodType = PeriodType.values().firstOrNull { it.extractor(filter.period) > 0 } ?: PeriodType.Months
        val initialAmount = initialPeriodType.extractor(filter.period)
        val periodTypeProperty = initialPeriodType.toProperty()
        val amountProperty = initialAmount.toProperty()
        plusMinusSlider(amountProperty, min = 0, max = 100)
        enumComboMenu(periodTypeProperty)
        amountProperty.combineLatest(periodTypeProperty).bindChanges(updateFilterActions) { (amount, periodType) ->
            filter to factory(Period().withField(periodType.fieldType, amount.toInt()))
        }
    }

    private fun HBox.renderFileSizeFilter(filter: Filter.FileSize) {
        val (initialAmount, initialScale) = filter.target.scaled
        val amountProperty = SimpleIntegerProperty(initialAmount.toInt())
        val scaleProperty = SimpleObjectProperty(initialScale)
        plusMinusSlider(amountProperty, min = 1, max = 999)
        enumComboMenu(scaleProperty)
        amountProperty.combineLatest(scaleProperty).bindChanges(updateFilterActions) { (amount, scale) ->
            filter to Filter.FileSize(FileSize(amount, scale))
        }
    }

    private fun HBox.renderFileNameFilter(filter: Filter.FileName, prevFilter: Filter?) {
        val textField = customTextField(filter.value)
        val value = textField.bindParser { it.toRegex() }
        value.typeSafeOnChange {
            it.valueOrNull?.let {
                updateFilterActions.event(filter to Filter.FileName(it.pattern))
            }
        }
        if (prevFilter is Filter.FileName) {
            val prevTextField = prevCache.getValue(prevFilter).children.find { it is CustomJFXTextField } as CustomJFXTextField
            if (prevTextField.isFocused) {
                textField.requestFocus()
                textField.positionCaret(prevTextField.caretPosition)
            }
        }
    }

    private fun renderNullFilter() {}

    private inline fun <Rule : Filter.Rule, T : Any> Rule.toProperty(
        extractor: Extractor<Rule, T>,
        crossinline factory: (T) -> Rule
    ): Property<T> = extractor(this).toProperty().apply { bindChanges(updateFilterActions) { this@toProperty to factory(it) }; Unit }

    enum class PeriodType(val fieldType: DurationFieldType, val extractor: (Period) -> Int) {
        Years(DurationFieldType.years(), Period::getYears),
        Months(DurationFieldType.months(), Period::getMonths),
        Days(DurationFieldType.days(), Period::getDays),
        Hours(DurationFieldType.hours(), Period::getHours),
        Minutes(DurationFieldType.minutes(), Period::getMinutes),
        Seconds(DurationFieldType.seconds(), Period::getSeconds);

        operator fun invoke(period: Period): Int = extractor(period)
    }

    private data class FilterDisplayDescriptor(
        val filter: KClass<out Filter>,
        val name: String,
        val icon: () -> FontIcon,
        val gap: Boolean = false,
        val actionIcon: () -> FontIcon = Icons::checked,
        val negatedActionIcon: () -> FontIcon = Icons::checkX,
        val subMenu: FilterDisplaySubMenu? = null
    ) {
        val selectedName = subMenu?.text ?: name
        val selectedIcon = subMenu?.icon ?: icon
    }

    private data class FilterDisplaySubMenu(
        val text: String,
        val icon: () -> FontIcon
    )

    private companion object {
        private val releaseDateSubMenu = FilterDisplaySubMenu("Release Date", Icons::date)
        private val createDateSubMenu = FilterDisplaySubMenu("Create Date", Icons::createDate)
        private val updateDateSubMenu = FilterDisplaySubMenu("Update Date", Icons::updateDate)

        private inline fun <reified T : Filter.TargetScore> score(name: String, noinline icon: () -> FontIcon, gap: Boolean = false) =
            FilterDisplayDescriptor(T::class, name, icon, gap, actionIcon = Icons::gtOrEq, negatedActionIcon = Icons::lt)

        private inline fun <reified T : Filter.TargetDate> targetDate(subMenu: FilterDisplaySubMenu, gap: Boolean = false) =
            FilterDisplayDescriptor(T::class, "Is after the given date", Icons::gtOrEq, gap, Icons::gtOrEq, Icons::lt, subMenu)

        private inline fun <reified T : Filter.PeriodDate> periodDate(subMenu: FilterDisplaySubMenu, gap: Boolean = false) =
            FilterDisplayDescriptor(T::class, "Is in a time period ending now", Icons::clockStart, gap, Icons::clockStart, Icons::clockEnd, subMenu)

        private inline fun <reified T : Filter> nullFilter(subMenu: FilterDisplaySubMenu, gap: Boolean = false) =
            FilterDisplayDescriptor(T::class, "Is null", Icons::checkX, gap = gap, actionIcon = Icons::checkX, negatedActionIcon = Icons::checked, subMenu = subMenu)

        val displayDescriptors = listOf(
            FilterDisplayDescriptor(Filter.Not::class, "Not", Icons::exclamation),
            FilterDisplayDescriptor(Filter.And::class, "And", Icons::and),
            FilterDisplayDescriptor(Filter.Or::class, "Or", Icons::or),
            FilterDisplayDescriptor(Filter.True::class, "Select Filter", Icons::select),

            FilterDisplayDescriptor(Filter.Platform::class, "Platform", Icons::computer, gap = true),

            FilterDisplayDescriptor(Filter.Library::class, "Library", Icons::folder),
            FilterDisplayDescriptor(Filter.Genre::class, "Genre", Icons::masks),
            FilterDisplayDescriptor(Filter.Tag::class, "Tag", { Icons.tag.color(Color.BLACK) }),
            FilterDisplayDescriptor(Filter.FilterTag::class, "Filter Tag", { Icons.tag.color(Color.BLACK) }),
            FilterDisplayDescriptor(Filter.Provider::class, "Provider", Icons::cloud, gap = true),

            score<Filter.CriticScore>("Critic Score", Icons::starFull),
            score<Filter.UserScore>("User Score", Icons::starEmpty),
            score<Filter.AvgScore>("Average Score", Icons::starHalf),
            score<Filter.MinScore>("Min Score", Icons::min),
            score<Filter.MaxScore>("Max Score", Icons::max, gap = true),

            targetDate<Filter.TargetReleaseDate>(releaseDateSubMenu),
            periodDate<Filter.PeriodReleaseDate>(releaseDateSubMenu),
            nullFilter<Filter.NullReleaseDate>(releaseDateSubMenu),
            targetDate<Filter.TargetCreateDate>(createDateSubMenu),
            periodDate<Filter.PeriodCreateDate>(createDateSubMenu),
            targetDate<Filter.TargetUpdateDate>(updateDateSubMenu),
            periodDate<Filter.PeriodUpdateDate>(updateDateSubMenu, gap = true),

            FilterDisplayDescriptor(Filter.FileName::class, "File Name", Icons::fileDocument, actionIcon = Icons::match, negatedActionIcon = Icons::notEqual),
            FilterDisplayDescriptor(Filter.FileSize::class, "File Size", Icons::fileQuestion, actionIcon = Icons::gtOrEq, negatedActionIcon = Icons::lt)
        ).map { it.filter to it }.toMap()
    }

    private val Filter.descriptor get() = this::class.descriptor
    private val KClass<out Filter>.descriptor get() = displayDescriptors.getValue(this)
}
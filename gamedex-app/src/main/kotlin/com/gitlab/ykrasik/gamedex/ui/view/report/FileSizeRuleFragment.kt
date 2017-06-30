package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.core.ReportRule
import com.gitlab.ykrasik.gamedex.ui.jfxToggleButton
import com.gitlab.ykrasik.gamedex.ui.map
import com.gitlab.ykrasik.gamedex.ui.mapBidirectional
import com.gitlab.ykrasik.gamedex.util.FileSize
import javafx.beans.property.Property
import javafx.geometry.Pos
import javafx.scene.control.TextField
import tornadofx.*

/**
 * User: ykrasik
 * Date: 01/07/2017
 * Time: 11:36
 */
class FileSizeRuleFragment(rule: ReportRule.Rules.HasFileSize) : Fragment() {
    val ruleProperty = rule.toProperty()

    private var textField: TextField by singleAssign()

    private val sizeTextProperty = FileSize(rule.target).humanReadable.toProperty()

    private val viewModel = FileSizeViewModel(sizeTextProperty).apply {
        textProperty.onChange { this@apply.commit() }
        validate(decorateErrors = true)
    }

    override val root = hbox {
        alignment = Pos.CENTER_LEFT
        textField = textfield(viewModel.textProperty) {
            isFocusTraversable = false
            validator {
                val valid = try {
                    FileSize(it!!); true
                } catch (e: Exception) {
                    false
                }
                if (!valid) error("Invalid file size! Format: {x} B KB MB GB TB PB EB") else null
            }
        }

        val isGt = ruleProperty.mapBidirectional(
            { it!!.greaterThan }, { ReportRule.Rules.HasFileSize(rule.target, it!!) }
        )
        jfxToggleButton {
            selectedProperty().bindBidirectional(isGt)
            textProperty().bind(isGt.map { if (it!!) ">=" else "<=" })
        }
    }

    val isValid = viewModel.valid

    init {
        // TODO: Use a bidirectional binding
        sizeTextProperty.onChange {
            ruleProperty.value = ReportRule.Rules.HasFileSize(FileSize(it!!).bytes, rule.greaterThan)
        }
    }

    private class FileSizeViewModel(p: Property<String>) : ViewModel() {
        val textProperty = bind { p }
    }
}
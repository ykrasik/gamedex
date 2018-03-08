package com.gitlab.ykrasik.gamedex.ui.view.game.filter

import com.gitlab.ykrasik.gamedex.core.Filter
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
class FileSizeRuleFragment(rule: Property<Filter.FileSize>) : Fragment() {
    private var textField: TextField by singleAssign()

    private val sizeTextProperty = rule.mapBidirectional({ target.humanReadable }, { Filter.FileSize(FileSize(this)) })

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
    }

    val isValid = viewModel.valid

    private class FileSizeViewModel(p: Property<String>) : ViewModel() {
        val textProperty = bind { p }
    }
}
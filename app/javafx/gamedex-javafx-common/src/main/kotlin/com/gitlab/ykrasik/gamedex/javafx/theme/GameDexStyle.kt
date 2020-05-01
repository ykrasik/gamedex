/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.javafx.theme

import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.ContentDisplay
import javafx.scene.effect.DropShadow
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * User: ykrasik
 * Date: 06/05/2017
 * Time: 13:43
 */
class GameDexStyle : Stylesheet() {
    companion object {
        val hiddenTabPaneHeader by cssclass()

        val centered by cssclass()

        val jfxHoverable by cssclass()
        val jfxToggleNodeLabel by cssclass()

        val card by cssclass()

        val toolbarButton by cssclass()
        val confirmButton by cssclass()
        val warningButton by cssclass()
        val dangerButton by cssclass()
        val infoButton by cssclass()

        val popOverMenu by cssclass()
        val popOverSubMenu by cssclass()

        val extraMenu by cssclass()

        val headerLabel by cssclass()
        val subHeaderLabel by cssclass()

        val toggleButtonCheckedText by cssclass()
        val toggleButtonUncheckedText by cssclass()

        val prettyToolbar by cssclass()

        val clearButton by cssclass()

        val prettyList by cssclass()
        val prettyListCellContent by cssclass()

        val prettyScrollPane by cssclass()
        val prettyScrollBar by cssclass()

        val prettyGridView by cssclass()

        val jfxButton by cssclass()
        val jfxProgressBar by csselement("JFXProgressBar")
        val jfxSpinner by csselement("JFXSpinner")
        val jfxDialog by cssclass()

        val stackPane by csselement("StackPane")

        val arc by cssclass()
        val secondaryBar by cssclass()

        val criticScore by cssclass()
        val criticScoreReviews by cssclass()
        val userScore by cssclass()
        val userScoreReviews by cssclass()

        val notificationBar by cssclass()

        val hoverShadow = mixin {
            and(hover) {
                effect = DropShadow()
            }
        }
    }

    init {
        val jfxHoverableMixin = mixin {
            and(hover) {
                backgroundColor = multi(Colors.blueGrey)
            }
        }

        val disableNativeScrollBarMixin = mixin {
            scrollBar {
                s(incrementArrow, incrementButton, incrementArrowButton, decrementArrow, decrementButton, decrementArrowButton) {
                    prefWidth = 0.px
                    prefHeight = 0.px
                }
                prefWidth = 0.px
                prefHeight = 0.px
            }
        }

        hiddenTabPaneHeader {
            tabMaxHeight = 0.px

            tabHeaderArea {
                visibility = FXVisibility.HIDDEN
            }
        }

        centered {
            alignment = Pos.CENTER
        }

        jfxHoverable {
            +jfxHoverableMixin
        }

        jfxToggleNodeLabel {
            padding = box(vertical = 0.px, horizontal = 5.px)
            alignment = Pos.CENTER_LEFT
        }

        card {
            borderRadius = multi(box(10.px))
            backgroundColor = multi(Colors.heavyRain)
            backgroundRadius = multi(box(10.px))
        }

        toolbarButton {
            minWidth = 100.px
            prefHeight = 40.px
        }

        confirmButton {
            and(hover) {
                backgroundColor = multi(Color.LIMEGREEN)
            }
        }

        warningButton {
            and(hover) {
                backgroundColor = multi(Color.DARKORANGE)
            }
        }

        dangerButton {
            and(hover) {
                backgroundColor = multi(Color.INDIANRED)
            }
        }

        infoButton {
            and(hover) {
                backgroundColor = multi(Color.CORNFLOWERBLUE)
            }
        }

        popOverMenu {
            backgroundColor = multi(Colors.cloudyKnoxville)
            spacing = 5.px
            padding = box(5.px)
        }

        popOverSubMenu {
            padding = box(4.px, 8.px, 4.px, 8.px)
            borderRadius = multi(box(3.px))
            backgroundRadius = multi(box(3.px))
        }

        extraMenu {
            prefWidth = 160.px
            contentDisplay = ContentDisplay.RIGHT
            alignment = Pos.CENTER_RIGHT
            graphicTextGap = 6.px
        }

        headerLabel {
            fontSize = 18.px
            fontWeight = FontWeight.BOLD
        }

        subHeaderLabel {
            fontSize = 15.px
            fontWeight = FontWeight.BOLD
        }

        toggleButtonCheckedText {
            and(checked) {
                fontWeight = FontWeight.BOLD
            }
        }

        toggleButtonUncheckedText {
            and(checked) {
                fontWeight = FontWeight.BOLD
            }
        }

        prettyToolbar {
            backgroundColor = multi(Colors.cloudyKnoxville)
            padding = box(6.px)
            borderWidth = multi(box(top = 0.px, bottom = 0.5.px, left = 0.px, right = 0.px))
            borderColor = multi(box(Color.GRAY))
        }

        clearButton {
            cursor = Cursor.DEFAULT
            backgroundRadius = multi(box(10.px))
            padding = box(0.px)
            and(hover) {
                backgroundColor = multi(Color.TRANSPARENT)
            }
            and(armed) {
                backgroundColor = multi(Color.TRANSPARENT)
            }
            and(pressed) {
                backgroundColor = multi(Color.TRANSPARENT)
            }
            and(selected) {
                backgroundColor = multi(Color.TRANSPARENT)
            }
        }

        prettyList {
            padding = box(0.px)
            backgroundInsets = multi(box(0.px))
            backgroundColor = multi(Color.TRANSPARENT) // removes default white background
            listCell {
                backgroundColor = multi(Color.TRANSPARENT) // removes alternating list gray cells.
                and(selected) {
                    backgroundColor = multi(Colors.prettyLightGray)
                    textFill = Color.BLACK
                    label {
                        textFill = Color.BLACK
                    }
                }
                +jfxHoverableMixin
                and(hover) {
                    and(empty) {
                        backgroundColor = multi(Color.TRANSPARENT)
                    }
                }
            }

            virtualFlow {
                +disableNativeScrollBarMixin
            }
        }

        prettyListCellContent {
            padding = box(8.px, 5.px)
        }

        prettyGridView {
            virtualFlow {
                +disableNativeScrollBarMixin
            }
        }

        jfxProgressBar {
            bar {
                backgroundColor = multi(Color.CORNFLOWERBLUE)
                backgroundRadius = multi(box(20.px))
            }
            secondaryBar {
                backgroundRadius = multi(box(20.px))
            }
        }

        jfxSpinner {
            percentage {
                fill = Color.ORANGE
            }
            arc {
                stroke = Color.ORANGE
            }
        }

        jfxDialog {
            stackPane {
                backgroundRadius = multi(box(20.px))
                backgroundColor = multi(Color.TRANSPARENT)
            }
        }

        spinner {
            promptTextFill = Color.BLACK
            textFill = Color.BLACK
            backgroundColor = multi(Color.TRANSPARENT)
            padding = box(0.px, 0.px, 3.px, 0.px)

            incrementArrowButton {
                backgroundColor = multi(Color.TRANSPARENT)
                incrementArrow {
                    backgroundColor = multi(c("#007cff"))
                    fontSize = 30.px
                }
            }
            decrementArrowButton {
                backgroundColor = multi(Color.TRANSPARENT)
                decrementArrow {
                    backgroundColor = multi(c("#007cff"))
                    fontSize = 30.px
                }
            }

            textField {
                backgroundColor = multi(Color.TRANSPARENT)
                borderWidth = multi(box(0.px, 0.px, 1.px, 0.px))
                borderColor = multi(box(c("#bdbdbd")))
            }
        }

        scrollPane {
            backgroundColor = multi(Color.TRANSPARENT)
            viewport {
                backgroundColor = multi(Color.TRANSPARENT)
            }
//            listView {
//                scrollBar {
//                    // Suppress the default ListView ScrollBar, when the ListView is wrapped in a ScrollPane.
//                    prefWidth = 0.px
//                }
//            }
        }

        scrollBar {
            padding = box(0.px)
            backgroundColor = multi(Color.TRANSPARENT)
            prefWidth = 1.em
            prefHeight = 1.em
            s(incrementArrow, incrementButton, incrementArrowButton, decrementArrow, decrementButton, decrementArrowButton) {
                prefWidth = 0.px
                prefHeight = 0.px
            }
            thumb {
                backgroundColor = multi(c(0, 0, 0, 0.5))
                backgroundRadius = multi(box(10.px))
                and(hover) {
                    backgroundColor = multi(c(0, 0, 0, 0.55))
                }
                and(pressed) {
                    backgroundColor = multi(c(0, 0, 0, 0.6))
                }
            }
        }

        criticScore {
            fontSize = 26.px
            fontWeight = FontWeight.BOLD
            textFill = Color.WHITE
        }

        criticScoreReviews {
            fontSize = 18.px
            textFill = Color.WHITE
        }

        userScore {
            fontSize = 18.px
            fontWeight = FontWeight.BOLD
            textFill = Color.WHITE
            padding = box(top = 6.px, right = 0.px, left = 0.px, bottom = 0.px)
        }

        userScoreReviews {
            fontSize = 15.px
            textFill = Color.WHITE
            padding = box(top = 4.px, right = 0.px, left = 0.px, bottom = 2.px)
        }

        notificationBar {
            minWidth = 400.px
        }
    }
}
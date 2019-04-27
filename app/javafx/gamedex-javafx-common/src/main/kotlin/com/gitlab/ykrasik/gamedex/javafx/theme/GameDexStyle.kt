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

package com.gitlab.ykrasik.gamedex.javafx.theme

import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 06/11/2018
 * Time: 14:35
 */
class GameDexStyle : Stylesheet() {
    companion object {
        val jfxButton by cssclass()
        val jfxProgressBar by csselement("JFXProgressBar")
        val jfxSpinner by csselement("JFXSpinner")

        val arc by cssclass()
        val secondaryBar by cssclass()
    }

    init {
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
            backgroundColor = multi(Color.WHITE)
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
    }
}
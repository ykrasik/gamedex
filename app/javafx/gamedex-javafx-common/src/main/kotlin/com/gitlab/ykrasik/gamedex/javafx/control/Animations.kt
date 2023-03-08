/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.javafx.control

import com.jfoenix.transitions.JFXFillTransition
import javafx.animation.FadeTransition
import javafx.animation.Interpolator
import javafx.animation.ScaleTransition
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.util.Duration
import tornadofx.fade
import tornadofx.point
import tornadofx.scale

/**
 * User: ykrasik
 * Date: 12/05/2019
 * Time: 21:16
 */
fun Node.flashFade(duration: Duration, target: Double = 0.0): FadeTransition {
    val from = opacity
    return fade(duration, opacity = target) {
        setOnFinished {
            fade(duration, opacity = from)
        }
    }
}

inline fun Region.fillTransition(
    duration: Duration,
    from: Color,
    to: Color,
    easing: Interpolator = Interpolator.EASE_BOTH,
    play: Boolean = true,
    op: JFXFillTransition.() -> Unit = {},
): JFXFillTransition = JFXFillTransition(duration, this, from, to).apply {
    interpolator = easing
    op()
    if (play) play()
}

fun Region.flashColor(
    duration: Duration,
    from: Color,
    to: Color,
    easing: Interpolator = Interpolator.EASE_BOTH,
): JFXFillTransition {
    return fillTransition(duration, from = from, to = to, easing = easing) {
        setOnFinished {
            fillTransition(duration, from = to, to = from, easing = easing)
        }
    }
}

fun Node.scaleOnMouseOver(duration: Duration, target: Double, from: Double = 1.0) {
    addEventFilter(MouseEvent.MOUSE_ENTERED) {
        if (!currentlyScalingUp && scaleX < target && scaleY < target) {
            currentScaleTransition?.stop()
            currentlyScalingDown = false
            currentlyScalingUp = true
            currentScaleTransition = scale(duration, point(target, target)) {
                setOnFinished {
                    currentlyScalingUp = false
                }
            }
        }
    }
    addEventFilter(MouseEvent.MOUSE_EXITED) {
        if (!currentlyScalingDown && scaleX > from && scaleY > from) {
            currentScaleTransition?.stop()
            currentlyScalingUp = false
            currentlyScalingDown = true
            currentScaleTransition = scale(duration, point(from, from)) {
                setOnFinished {
                    currentlyScalingDown = false
                }
            }
        }
    }
}

fun Node.flashScale(duration: Duration, target: Double, from: Double = 1.0) {
    fun scaleDown(): Boolean =
        if (!currentlyScalingDown && scaleX > from && scaleY > from) {
            currentScaleTransition?.stop()
            currentlyScalingUp = false
            currentlyScalingDown = true
            currentScaleTransition = scale(duration, point(from, from)) {
                setOnFinished {
                    currentlyScalingDown = false
                    currentScaleTransition = null
                }
            }
            true
        } else {
            false
        }

    fun scaleUp(): Boolean =
        if (!currentlyScalingUp && scaleX < target && scaleY < target) {
            currentlyScalingDown = false
            currentlyScalingUp = true
            currentScaleTransition = scale(duration, point(target, target)) {
                setOnFinished {
                    currentlyScalingUp = false
                    scaleDown()
                }
            }
            true
        } else {
            false
        }

    if (!scaleUp() && !currentlyScalingUp) {
        scaleDown()
    }
}

private var Node.currentlyScalingUp: Boolean
    get() = properties["gameDex.currentlyScalingUp"] as? Boolean ?: false
    set(value) {
        properties["gameDex.currentlyScalingUp"] = value
    }

private var Node.currentlyScalingDown: Boolean
    get() = properties["gameDex.currentlyScalingDown"] as? Boolean ?: false
    set(value) {
        properties["gameDex.currentlyScalingDown"] = value
    }

private var Node.currentScaleTransition: ScaleTransition?
    get() = properties["gameDex.currentScaleTransition"] as? ScaleTransition
    set(value) {
        properties["gameDex.currentScaleTransition"] = value
    }
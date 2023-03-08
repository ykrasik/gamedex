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

package com.gitlab.ykrasik.gamedex.javafx.theme

import com.gitlab.ykrasik.gamedex.Score
import javafx.animation.Interpolator
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import tornadofx.c

/**
 * User: ykrasik
 * Date: 01/12/2018
 * Time: 20:43
 */
object Colors {
    val red = Color.DARKRED.brighter()
    val green = Color.DARKGREEN
    val orange = Color.ORANGE.darker()
    val blue = Color.CORNFLOWERBLUE
    val niceBlue = c("#93ADCA")
    val niceBlueDarker = c("#6e85a0")
    val prettyLightGray = c("#cfd9df")
    val anotherPrettyLightGray = c("#bfc4cc")
    val blueGrey = c("#afbbce")
    val transparentDark = c(0, 0, 0, 0.6)
    val transparentWhite = c("#e8eaed", 0.6)

    // Taken from [https://digitalsynopsis.com/design/beautiful-color-gradients-backgrounds/]

    val heavyRain = linearGradient("#cfd9df", "#e2ebf0")
    val cloudyKnoxville = linearGradient("#fdfbfb", "#ebedee")
    val winterNeva = linearGradient("#a1c4fd", "#c2e9fb")
    val stPeteresberg = linearGradient("#f5f7fa", "#c3cfe2")
    val everlastingSky = linearGradient("#fdfcfb", "#e2d1c3")
    val greatWhale = linearGradient("#a3bded", "#6991c7")
    val cleanMirror = linearGradient("#93a5cf", "#e4efe9")
    val premiumDark = linearGradient("#434343", "#000000")
    val cochitiLake = linearGradient("#93a5cf", "#e4efe9")
    val mountainRock = linearGradient("#596164", "#868f96")
    val eternalConstance = linearGradient("#09203f", "#537895", "#09203f", direction = GradientDirection.Right)
    val viciousStance = linearGradient("#29323c", "#485563")
    val nightSky = linearGradient("#1e3c72", "#2a5298")
    val shadesOfGrey = linearGradient("#2c3e50", "#bdc3c7", "#2c3e50", direction = GradientDirection.Right)
    val decent = linearGradient("#4ca1af", "#c4e0e5", direction = GradientDirection.Down)
    val paleWood = linearGradient("#eacda3", "#d6ae7b")
    val desertHump = linearGradient("#c79081", "#dfa579")
}

fun linearGradient(vararg colors: String, direction: GradientDirection = GradientDirection.Down): LinearGradient {
    val stops = colors.mapIndexed { index, color -> Stop(index.toDouble() / (colors.size - 1), c(color)) }
    var startX = 0.0
    var startY = 0.0
    var endX = 0.0
    var endY = 0.0
    when (direction) {
        GradientDirection.Up -> {
            startY = 1.0
        }
        GradientDirection.Down -> {
            endY = 1.0
        }
        GradientDirection.Left -> {
            startX = 1.0
        }
        GradientDirection.Right -> {
            endX = 1.0
        }
        GradientDirection.UpRight -> {
            startY = 1.0
            endX = 1.0
        }
        GradientDirection.UpLeft -> {
            startY = 1.0
            startX = 1.0
        }
        GradientDirection.DownRight -> {
            endX = 1.0
            endY = 1.0
        }
        GradientDirection.DownLeft -> {
            startX = 1.0
            endY = 1.0
        }
    }
    return LinearGradient(startX, startY, endX, endY, true, CycleMethod.NO_CYCLE, stops)
}

enum class GradientDirection {
    Up,
    Down,
    Left,
    Right,
    UpRight,
    UpLeft,
    DownRight,
    DownLeft
}

val Score?.ratingColor: Color
    get() = if (this != null) (score / 100).ratingColor else Colors.niceBlueDarker

val Double.ratingColor: Color
    get() = when {
        this < 0.3 -> Color.rgb(255, 0, 0)
        this < 0.5 -> {
            val normalized = (this - 0.3) / 0.2
            Color.rgb(255, normalized.interpolate(0, 150), 0)
        }
        else -> {
            val normalized = (this - 0.5) / 0.5
            Color.rgb(normalized.interpolate(255, 0), normalized.interpolate(150, 200), 0)
        }
    }

fun Double.interpolate(start: Int, end: Int) = Interpolator.LINEAR.interpolate(start, end, this)
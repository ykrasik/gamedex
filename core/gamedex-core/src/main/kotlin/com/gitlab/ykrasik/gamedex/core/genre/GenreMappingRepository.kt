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

package com.gitlab.ykrasik.gamedex.core.genre

import com.gitlab.ykrasik.gamedex.GenreId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/10/2019
 * Time: 13:42
 */
@Singleton
class GenreMappingRepository @Inject constructor() {
    val mapping: Map<GenreId, List<GenreId>> = defaultMapping

    private companion object {
        val defaultMapping = mapOf(
            "Action-Adventure" to listOf("Action", "Adventure"),
            "Action Adventure" to listOf("Action", "Adventure"),
            "Action RPG" to listOf("Action", "Role-Playing Game (RPG)"),
            "Text Adventure" to listOf("Adventure", "Text"),

            "Board Games" to listOf("Board / Card Game"),
            "Card Battle" to listOf("Board / Card Game"),
            "Card Game" to listOf("Board / Card Game"),
            "Gambling" to listOf("Board / Card Game"),
            "Quiz/Trivia" to listOf("Board / Card Game"),
            "Parlor" to listOf("Board / Card Game"),
            "Trivia/Board Game" to listOf("Board / Card Game"),

            "Minigame Collection" to listOf("Compilation"),

            "Driving/Racing" to listOf("Driving / Racing"),
            "Driving" to listOf("Driving / Racing"),
            "Automobile" to listOf("Driving / Racing"),
            "Car Combat" to listOf("Driving / Racing"),
            "GT / Street" to listOf("Driving / Racing"),
            "Motocross" to listOf("Driving / Racing"),
            "Motorcycle" to listOf("Driving / Racing"),
            "Racing" to listOf("Driving / Racing"),
            "Vehicle" to listOf("Driving / Racing"),
            "Vehicular Combat" to listOf("Driving / Racing", "Shooter"),
            "Vehicle Combat" to listOf("Driving / Racing", "Shooter"),

            "First-Person" to listOf("First-Person Shooter"),

            "Hack & Slash/Beat 'Em Up" to listOf("Hack & Slash / Beat 'Em Up"),
            "Beat-'Em-Up" to listOf("Hack & Slash / Beat 'Em Up"),
            "Brawler" to listOf("Hack & Slash / Beat 'Em Up"),
            "Fighting" to listOf("Hack & Slash / Beat 'Em Up"),

            "City Building" to listOf("Management"),
            "Business / Tycoon" to listOf("Management"),
            "Breeding/Constructing" to listOf("Management"),
            "Career" to listOf("Management"),
            "Government" to listOf("Management"),

            "Music/Rhythm" to listOf("Music / Rhythm"),
            "Music" to listOf("Music / Rhythm"),

            "Logic" to listOf("Puzzle"),
            "Matching" to listOf("Puzzle"),

            "Real-Time Strategy" to listOf("Real Time Strategy (RTS)"),

            "Role-Playing" to listOf("Role-Playing Game (RPG)"),
            "RPG" to listOf("Role-Playing Game (RPG)"),
            "Console-style RPG" to listOf("Role-Playing Game (RPG)"),
            "PC-style RPG" to listOf("Role-Playing Game (RPG)"),
            "Japanese-Style" to listOf("Role-Playing Game (RPG)"),
            "Western-Style" to listOf("Role-Playing Game (RPG)"),

            "Massively Multiplayer" to listOf("Massively Multiplayer (MMORPG)"),
            "MMORPG" to listOf("Massively Multiplayer (MMORPG)"),

            "Shoot 'Em Up" to listOf("Shooter"),
            "Shoot-'Em-Up" to listOf("Shooter"),
            "Artillery" to listOf("Shooter"),

            "Third-Person" to listOf("Third-Person Shooter"),

            "Flight Simulator" to listOf("Simulation"),
            "Flight" to listOf("Simulation"),
            "Small Spaceship" to listOf("Simulation"),
            "Large Spaceship" to listOf("Simulation"),
            "Virtual Life" to listOf("Simulation"),
            "Virtual" to listOf("Simulation"),
            "Train" to listOf("Simulation"),
            "Rail" to listOf("Simulation"),

            "Sports" to listOf("Sport"),
            "Football" to listOf("Sport"),
            "Boxing" to listOf("Sport"),
            "Bowling" to listOf("Sport"),
            "Basketball" to listOf("Sport"),
            "Golf" to listOf("Sport"),
            "Fishing" to listOf("Sport"),
            "Baseball" to listOf("Sport"),
            "Skateboarding" to listOf("Sport"),
            "Track & Field" to listOf("Sport"),

            "Defense" to listOf("Strategy"),
            "Military" to listOf("Strategy"),
            "Command" to listOf("Strategy"),
            "Tactical" to listOf("Strategy"),
            "Tactics" to listOf("Strategy"),
            "Wargame" to listOf("Strategy"),

            "Turn-Based" to listOf("Turn-Based Strategy (TBS)"),
            "Turn-Based Strategy" to listOf("Turn-Based Strategy (TBS)"),

            "Art" to emptyList(),
            "Creation" to emptyList(),
            "Indie" to emptyList(),
            "General" to emptyList(),
            "Miscellaneous" to emptyList(),
            "Modern" to emptyList(),
            "Marine" to emptyList(),
            "Traditional" to emptyList(),
            "Horizontal" to emptyList(),
            "Vertical" to emptyList(),
            "Linear" to emptyList(),
            "Other" to emptyList(),
            "Scrolling" to emptyList(),
            "Static" to emptyList(),
            "Civilian" to emptyList(),
            "Individual" to emptyList(),
            "Team" to emptyList(),
            "3D" to emptyList(),
            "Top-Down" to emptyList(),
            "Combat" to emptyList(),
            "Dual-Joystick Shooter" to emptyList(),
            "Educational" to emptyList(),
            "Real-Time" to emptyList(),
            "Light Gun" to emptyList(),
            "Pet" to emptyList(),
            "Pinball" to emptyList(),
            "Block-Breaking" to emptyList(),
            "Historic" to emptyList(),
            "Futuristic" to emptyList(),
            "Fantasy" to emptyList(),
            "Sci-Fi" to emptyList(),
            "Space" to emptyList()
        )
    }
}
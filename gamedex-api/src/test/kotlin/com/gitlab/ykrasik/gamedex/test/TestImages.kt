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

package com.gitlab.ykrasik.gamedex.test

import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner

/**
 * User: ykrasik
 * Date: 19/03/2017
 * Time: 13:51
 *
 * These images were all taken from igdb.com
 */
object TestImages {
    private val images = Reflections("com.gitlab.ykrasik.gamedex.test.images", ResourcesScanner())
        .getResources { true }
        .map { this::class.java.getResource("/$it") }

    fun randomImage(): ByteArray = images.randomElement().readBytes()
}
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

package com.gitlab.ykrasik.gamedex.util

import io.kotlintest.specs.StringSpec

/**
 * User: ykrasik
 * Date: 10/02/2017
 * Time: 10:58
 */
class HttpUtilsTest : StringSpec() {
    init {
        "Stream download" {
            repeat(2) {
                var chunks = 0
                val bytes = download("http://www.giantbomb.com/api/image/scale_avatar/2739447-assassins-creed-unity-china-chronicles-1.jpg", stream = true) { downloaded, total ->
                    chunks += 1
                    println("[$chunks] Download progress: $downloaded/$total")
                }
                assert(bytes.size == 11731) { "Invalid size: ${bytes.size}, expected = 11731"}
                println("*******************************")
            }
        }
    }
}
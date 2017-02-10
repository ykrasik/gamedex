package com.gitlab.ykrasik.gamedex.common.util

import io.kotlintest.specs.StringSpec

/**
 * User: ykrasik
 * Date: 10/02/2017
 * Time: 10:58
 */
class HttpUtilsTest : StringSpec() {
    init {
        "Stream download" {
            for (i in 1..5) {
                val bytes = download("http://www.giantbomb.com/api/image/scale_avatar/2739447-assassins-creed-unity-china-chronicles-1.jpg", stream = true) { downloaded, total ->
                    println("Download progress: $downloaded/$total")
                }
                assert(bytes.size == 11731) { "Invalid size: ${bytes.size}, expected = 11731"}
                println("*******************************")
            }
        }
    }
}
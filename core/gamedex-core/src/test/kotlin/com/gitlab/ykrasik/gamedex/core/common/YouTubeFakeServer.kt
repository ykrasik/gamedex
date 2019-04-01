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

package com.gitlab.ykrasik.gamedex.core.common

import com.gitlab.ykrasik.gamedex.plugin.DefaultPlugin
import com.gitlab.ykrasik.gamedex.plugin.PluginDescriptor
import com.gitlab.ykrasik.gamedex.test.FakeServerFactory
import com.gitlab.ykrasik.gamedex.test.KtorFakeServer
import com.gitlab.ykrasik.gamedex.util.freePort
import com.google.inject.Provides
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 21/01/2019
 * Time: 08:19
 */
class YouTubeFakeServer(port: Int = freePort) : KtorFakeServer(port) {
    override fun Application.setupServer() {
        routing {
            get("/results") {
                call.respondText(
                    """
                        <html>
                            <body>
                                ${call.parameters["search_query"]}
                            </body>
                        </html>
                    """.trimIndent(),
                    ContentType.Text.Html
                )
            }
        }
    }

    override fun setupEnv() {
        System.setProperty("gameDex.common.youTubeBaseUrl", baseUrl)
    }
}

@Suppress("unused")
object YouTubeTestkitPlugin : DefaultPlugin() {
    override val descriptor = PluginDescriptor(
        id = "youtube.testkit",
        author = "Yevgeny Krasik",
        version = "0.1.0"
    )

    @Provides
    @Singleton
    fun youTubeFakeServerFactory() = object : FakeServerFactory {
        override fun create(port: Int) = YouTubeFakeServer(port)
    }
}
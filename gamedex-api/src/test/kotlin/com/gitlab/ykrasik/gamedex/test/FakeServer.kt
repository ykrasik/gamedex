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

package com.gitlab.ykrasik.gamedex.test

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.freePort
import com.gitlab.ykrasik.gamedex.util.logger
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import java.io.Closeable

/**
 * User: ykrasik
 * Date: 21/01/2019
 * Time: 09:29
 */
interface FakeServer : Closeable {
    val port: Int

    fun setupEnv()

    fun start()
}

interface GameProviderFakeServer : FakeServer {
    val id: ProviderId
    val supportedPlatforms: List<Platform>
    fun randomProviderGameId(): String
    val thumbnailUrl: String?
    val posterUrl: String?
    val screenshotUrl: String?
}

abstract class KtorFakeServer(final override val port: Int = freePort) : FakeServer {
    private val logger = logger()

    val baseUrl = "http://localhost:$port"

    protected val ktor = embeddedServer(Netty, port) {
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                logger.error("Error", cause)
                call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
                throw cause
            }
        }
        setupServer(this)
    }

    protected abstract fun setupServer(app: Application): Any

    override fun start() {
        ktor.start()
    }

    override fun close() = ktor.stop(gracePeriodMillis = 100, timeoutMillis = 100)
}

interface FakeServerFactory {
    val preferredPort: Int? get() = null

    fun create(port: Int): FakeServer
}

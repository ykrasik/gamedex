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

package com.gitlab.ykrasik.gamedex.core.common

import com.gitlab.ykrasik.gamedex.test.FakeServer
import com.gitlab.ykrasik.gamedex.test.FakeServerFactory
import com.gitlab.ykrasik.gamedex.test.GameProviderFakeServer
import com.gitlab.ykrasik.gamedex.util.freePort
import com.gitlab.ykrasik.gamedex.util.isPortAvailable
import java.net.ServerSocket

/**
 * User: ykrasik
 * Date: 21/01/2019
 * Time: 10:07
 */
class FakeServerManager(fakeServerFactories: List<FakeServerFactory>) : FakeServer {
    private val promisedPorts = mutableMapOf<Int, ServerSocket>()

    private val fakeServers = fakeServerFactories.map { factory ->
        val preferredPort = factory.preferredPort
        val port = if (preferredPort != null && isPortAvailable(preferredPort)) preferredPort else freePort
        promisedPorts += port to ServerSocket(port)
        factory.create(port)
    }

    val providerFakeServers = fakeServers.mapNotNull { it as? GameProviderFakeServer }.let {
        require(it.isNotEmpty()) { "No game provider fake servers detected!" }
        it
    }

    override fun setupEnv() {
        fakeServers.forEach { it.setupEnv() }
    }

    override fun start() {
        fakeServers.forEach { server ->
            promisedPorts.remove(server.port)!!.close()
            println("Starting ${server::class.simpleName} on port ${server.port}...")
            server.start()
        }
    }

    override fun close() {
        fakeServers.forEach { it.close() }
    }

    override val port = 0
}

inline fun FakeServerManager.using(f: FakeServerManager.() -> Unit) {
    setupEnv()
    use {
        start()
        f()
    }
}

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

package com.gitlab.ykrasik.gamedex.test

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.UserAgent

/**
 * User: ykrasik
 * Date: 12/01/2019
 * Time: 09:50
 */
val testHttpClient = HttpClient(Apache) {
    expectSuccess = false
    install(UserAgent)

    engine {
        followRedirects = true  // Follow HTTP Location redirects - default false. It uses the default number of redirects defined by Apache's HttpClient that is 50.

        // For timeouts: 0 means infinite, while negative value mean to use the system's default value
        socketTimeout = 30_000  // Max time between TCP packets - default 10 seconds
        connectTimeout = 30_000 // Max time to establish an HTTP connection - default 10 seconds
        connectionRequestTimeout = 30_000 // Max time for the connection manager to start a request - 20 seconds
    }
}
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

package com.gitlab.ykrasik.gamedex.util

import com.gitlab.ykrasik.gamedex.util.ktor.JacksonSerializer
import com.gitlab.ykrasik.gamedex.util.ktor.Logging
import com.google.common.net.UrlEscapers
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.UserAgent
import io.ktor.client.features.json.Json
import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readBytes
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.*
import java.util.*

/**
 * User: ykrasik
 * Date: 10/02/2017
 * Time: 10:24
 */
val httpClient = HttpClient(Apache) {
    install(UserAgent)
    Logging {
        logger = logger("HttpClient")
    }
    Json {
        serializer = JacksonSerializer
    }
    engine {
        followRedirects = true  // Follow HTTP Location redirects - default false. It uses the default number of redirects defined by Apache's HttpClient that is 50.

        // For timeouts: 0 means infinite, while negative value mean to use the system's default value
        socketTimeout = 30_000  // Max time between TCP packets - default 10 seconds
        connectTimeout = 30_000 // Max time to establish an HTTP connection - default 10 seconds
        connectionRequestTimeout = 30_000 // Max time for the connection manager to start a request - 20 seconds
    }
}

suspend fun HttpClient.download(url: String, progressHandler: ((downloaded: Int, total: Int) -> Unit)? = null): ByteArray {
    val response = get<HttpResponse>(url)
    if (!response.status.isSuccess()) throw IllegalStateException("Download '$url': ${response.status}")
    return if (progressHandler != null) {
        val total = response.contentLength()?.toInt() ?: -1
        val contentLength = if (total != -1) total else 32.kb
        val os = ByteArrayOutputStream(contentLength)
        response.content.consumeEachBufferRange { buffer, last ->
            val array = ByteArray(buffer.remaining())
            buffer.get(array)
            os.write(array)
            progressHandler(os.size(), total)
            !last
        }
        os.toByteArray()
    } else {
        response.readBytes()
    }
}

fun String.urlEncoded() = UrlEscapers.urlFragmentEscaper().escape(this)
fun String.urlDecoded() = URLDecoder.decode(this, "utf-8")

fun String.base64Encoded() = Base64.getUrlEncoder().encodeToString(this.toByteArray())
fun String.base64Decoded() = String(Base64.getUrlDecoder().decode(this))

fun String.toUrl() = URL(this)

val URL.filePath get() = File(path)
val URL.fileName get() = File(path).name

val freePort get() = ServerSocket(0).use { it.localPort }
fun isPortAvailable(port: Int): Boolean = try {
    ServerSocket().use { serverSocket ->
        // setReuseAddress(false) is required only on OSX,
        // otherwise the code will not work correctly on that platform
        serverSocket.reuseAddress = false
        serverSocket.bind(InetSocketAddress(InetAddress.getByName("localhost"), port), 1)
        true
    }
} catch (ex: Exception) {
    false
}
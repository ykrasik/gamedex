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

import com.google.common.net.UrlEscapers
import khttp.get
import khttp.responses.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLDecoder
import java.util.*

/**
 * User: ykrasik
 * Date: 10/02/2017
 * Time: 10:24
 */
inline fun <reified T : Any> Response.fromJson(errorParser: (String) -> String = String::toString): T =
    doIfOk(errorParser) { content.fromJson() }

inline fun <reified T : Any> Response.listFromJson(errorParser: (String) -> String = String::toString): List<T> =
    doIfOk(errorParser) { content.listFromJson() }

inline fun <T> Response.doIfOk(errorParser: (String) -> String = String::toString, f: Response.() -> T): T {
    assertOk(errorParser)
    return f()
}

inline fun Response.assertOk(errorParser: (String) -> String = String::toString) = apply {
    if (statusCode != 200) {
        val errorMessage = if (text.isNotEmpty()) {
            ": ${errorParser(text)}"
        } else {
            ""
        }
        throw IllegalStateException("[${request.method}] ${request.url} returned $statusCode$errorMessage")
    }
}

fun download(url: String,
             stream: Boolean = false,
             progress: (downloaded: Int, total: Int) -> Unit = { _, _ -> }): ByteArray =
    try {
        val response = get(url, params = emptyMap(), headers = emptyMap(), stream = stream).assertOk()
        if (stream) {
            val contentLength = response.headers["Content-Length"]?.toInt() ?: 32.kb
            val os = ByteArrayOutputStream(contentLength)
            for (chunk in response.contentIterator(8.kb)) {
                os.write(chunk)
                progress(os.size(), contentLength)
            }
            os.toByteArray()
        } else {
            response.content
        }
    } catch (e: SocketTimeoutException) {
        throw SocketTimeoutException("Timed out downloading: $url")
    }

fun String.urlEncoded() = UrlEscapers.urlFragmentEscaper().escape(this)
fun String.urlDecoded() = URLDecoder.decode(this, "utf-8")

fun String.base64Encoded() = Base64.getUrlEncoder().encodeToString(this.toByteArray())
fun String.base64Decoded() = String(Base64.getUrlDecoder().decode(this))

fun String.toUrl() = URL(this)

val URL.filePath get() = File(path)
val URL.fileName get() = File(path).name
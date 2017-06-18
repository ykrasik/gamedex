package com.gitlab.ykrasik.gamedex.util

import com.gitlab.ykrasik.gamedex.GamedexException
import khttp.get
import khttp.responses.Response
import java.io.ByteArrayOutputStream
import java.net.SocketTimeoutException

/**
 * User: ykrasik
 * Date: 10/02/2017
 * Time: 10:24
 */
inline fun <reified T : Any> Response.fromJson(noinline errorParser: (String) -> String = String::toString): T =
    doIfOk(errorParser) { content.fromJson() }

inline fun <reified T : Any> Response.listFromJson(noinline errorParser: (String) -> String = String::toString): List<T> =
    doIfOk(errorParser) { content.listFromJson() }

fun <T> Response.doIfOk(errorParser: (String) -> String = String::toString, f: Response.() -> T): T {
    assertOk(errorParser)
    return f()
}

fun Response.assertOk(errorParser: (String) -> String = String::toString) {
    if (statusCode != 200) {
        val errorMessage = if (text.isNotEmpty()) {
            ": ${errorParser(text)}"
        } else {
            ""
        }
        throw GamedexException("[${request.method}] ${request.url} returned $statusCode$errorMessage")
    }
}

fun download(url: String,
             stream: Boolean = false,
             progress: (downloaded: Int, total: Int) -> Unit = { _, _ -> }): ByteArray {
    val response = try {
        get(url, params = emptyMap(), headers = emptyMap(), stream = stream)
    } catch (e: SocketTimeoutException) {
        throw SocketTimeoutException("Timed out downloading: $url")
    }
    return response.doIfOk {
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
    }
}
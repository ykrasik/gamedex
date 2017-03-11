package com.gitlab.ykrasik.gamedex.common.util

import com.gitlab.ykrasik.gamedex.common.exception.GameDexException
import khttp.get
import khttp.responses.Response
import java.io.ByteArrayOutputStream

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
        throw GameDexException("[${request.method}] ${request.url} returned $statusCode$errorMessage")
    }
}

fun download(url: String,
             stream: Boolean = true,
             params: Map<String, String> = emptyMap(),
             headers: Map<String, String> = emptyMap(),
             progress: (downloaded: Int, total: Int) -> Unit = { _, _ -> }): ByteArray {
    val response = get(url, params = params, headers = headers, stream = stream)
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
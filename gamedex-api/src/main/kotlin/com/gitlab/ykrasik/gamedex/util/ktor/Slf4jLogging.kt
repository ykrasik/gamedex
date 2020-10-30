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

package com.gitlab.ykrasik.gamedex.util.ktor

import com.gitlab.ykrasik.gamedex.util.humanReadable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.features.observer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.core.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.Logger
import kotlin.text.Charsets
import kotlin.text.String
import kotlin.text.StringBuilder
import kotlin.text.appendLine
import kotlin.text.startsWith
import kotlin.time.milliseconds

/**
 * User: ykrasik
 * Date: 15/06/2019
 * Time: 15:43
 */
class Slf4jLogging(val logger: Logger) {
    class Config {
        lateinit var logger: Logger
    }

    companion object Feature : HttpClientFeature<Config, Slf4jLogging> {
        override val key: AttributeKey<Slf4jLogging> = AttributeKey("Slf4jLogging")

        private val requestLog = AttributeKey<String>("RequestLog")

        override fun prepare(block: Config.() -> Unit): Slf4jLogging {
            val config = Config().apply(block)
            return Slf4jLogging(config.logger)
        }

        override fun install(feature: Slf4jLogging, scope: HttpClient) {
            scope.sendPipeline.intercept(HttpSendPipeline.Before) {
                runCatching {
                    context.attributes.put(requestLog, feature.requestMessage(context))
                }

                try {
                    proceedWith(subject)
                } catch (e: Throwable) {
                    feature.logRequestException(context, e)
                    throw e
                }
            }

            scope.responsePipeline.intercept(HttpResponsePipeline.Receive) {
                try {
                    proceedWith(subject)
                } catch (cause: Throwable) {
                    feature.logResponseException(context, cause)
                    throw cause
                }
            }

            val observer: ResponseHandler = {
                runCatching { feature.logResponse(it) }
            }

            ResponseObserver.install(ResponseObserver(observer), scope)
        }
    }

    private suspend fun requestMessage(request: HttpRequestBuilder): String {
        val sb = StringBuilder().appendLine()
        sb.appendLine("REQUEST: [${request.method.value}] ${Url(request.url)}")
        val content = request.body as OutgoingContent
//        sb.logHeaders(request.headers.entries(), content.headers)
        sb.logRequestBody(content)
        return sb.toString()
    }

    private suspend fun logResponse(response: HttpResponse) {
        val sb = StringBuilder(response.call.request.attributes[requestLog]).appendLine()
        sb.appendLine("RESPONSE: [${response.status}] ${response.timeTaken}")
//        sb.logHeaders(response.headers.entries())
        sb.logResponseBody(response.contentType(), response.content)

        logger.trace(sb.toString())
    }

    private fun logRequestException(request: HttpRequestBuilder, e: Throwable) {
        logger.error(request.attributes[requestLog] + "REQUEST ERROR", e)
    }

    private fun logResponseException(call: HttpClientCall, e: Throwable) {
        logger.error(call.request.attributes[requestLog] + "RESPONSE ERROR ${call.response.timeTaken}", e)
    }

//    private fun StringBuilder.logHeaders(
//        requestHeaders: Set<Map.Entry<String, List<String>>>,
//        contentHeaders: Headers? = null
//    ) {
//        appendLine("COMMON HEADERS")
//        requestHeaders.forEach { (key, values) ->
//            appendLine("-> $key: ${values.joinToString("; ")}")
//        }
//
//        if (contentHeaders != null && !contentHeaders.isEmpty()) {
//            appendLine("CONTENT HEADERS")
//            contentHeaders.forEach { key, values ->
//                appendLine("-> $key: ${values.joinToString("; ")}")
//            }
//        }
//    }

    private suspend fun StringBuilder.logResponseBody(contentType: ContentType?, content: ByteReadChannel) {
        appendLine("Content-Type: $contentType")
        val message = content.readText(contentType?.charset() ?: Charsets.UTF_8)
        if (message.startsWith('{') || message.startsWith('[')) {
            appendLine(message)
        } else {
            appendLine("BODY NOT JSON")
        }
    }

    private suspend fun StringBuilder.logRequestBody(content: OutgoingContent) {
        val charset = content.contentType?.charset() ?: Charsets.UTF_8

        val text = when (content) {
            is OutgoingContent.WriteChannelContent -> {
                val textChannel = ByteChannel()
                GlobalScope.launch(Dispatchers.Unconfined) {
                    content.writeTo(textChannel)
                    textChannel.close(null)
                }
                textChannel.readText(charset)
            }
            is OutgoingContent.ReadChannelContent -> {
                content.readFrom().readText(charset)
            }
            is OutgoingContent.ByteArrayContent -> String(content.bytes(), charset = charset)
            else -> null
        }

        if (text != null) {
            appendLine("Content-Type: ${content.contentType}")
            appendLine(text)
        }
    }

    private suspend inline fun ByteReadChannel.readText(charset: Charset): String =
        readRemaining().readText(charset = charset)

    private val HttpResponse.timeTaken: String get() = (responseTime.timestamp - requestTime.timestamp).milliseconds.humanReadable
}

/**
 * Install [Slf4jLogging].
 */
fun HttpClientConfig<*>.Logging(block: Slf4jLogging.Config.() -> Unit) {
    install(Slf4jLogging, block)
}
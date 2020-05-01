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
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.HttpClientCall
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.features.observer.ResponseHandler
import io.ktor.client.features.observer.ResponseObserver
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.HttpResponsePipeline
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.util.AttributeKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.io.ByteChannel
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.close
import kotlinx.coroutines.io.readRemaining
import kotlinx.coroutines.launch
import kotlinx.io.charsets.Charset
import kotlinx.io.core.readText
import kotlinx.io.core.use
import org.slf4j.Logger
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
        val sb = StringBuilder().appendln()
        sb.appendln("REQUEST: [${request.method.value}] ${Url(request.url)}")
        val content = request.body as OutgoingContent
//        sb.logHeaders(request.headers.entries(), content.headers)
        sb.logRequestBody(content)
        return sb.toString()
    }

    private suspend fun logResponse(response: HttpResponse): Unit = response.use {
        val sb = StringBuilder(response.call.request.attributes[requestLog]).appendln()
        sb.appendln("RESPONSE: [${response.status}] ${response.timeTaken}")
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
//        appendln("COMMON HEADERS")
//        requestHeaders.forEach { (key, values) ->
//            appendln("-> $key: ${values.joinToString("; ")}")
//        }
//
//        if (contentHeaders != null && !contentHeaders.isEmpty()) {
//            appendln("CONTENT HEADERS")
//            contentHeaders.forEach { key, values ->
//                appendln("-> $key: ${values.joinToString("; ")}")
//            }
//        }
//    }

    private suspend fun StringBuilder.logResponseBody(contentType: ContentType?, content: ByteReadChannel) {
        appendln("Content-Type: $contentType")
        val message = content.readText(contentType?.charset() ?: Charsets.UTF_8)
        if (message.startsWith('{') || message.startsWith('[')) {
            appendln(message)
        } else {
            appendln("BODY NOT JSON")
        }
    }

    private suspend fun StringBuilder.logRequestBody(content: OutgoingContent) {
        val charset = content.contentType?.charset() ?: Charsets.UTF_8

        val text = when (content) {
            is OutgoingContent.WriteChannelContent -> {
                val textChannel = ByteChannel()
                GlobalScope.launch(Dispatchers.Unconfined) {
                    content.writeTo(textChannel)
                    textChannel.close()
                }
                textChannel.readText(charset)
            }
            is OutgoingContent.ReadChannelContent -> {
                content.readFrom().readText(charset)
            }
            is OutgoingContent.ByteArrayContent -> kotlinx.io.core.String(content.bytes(), charset = charset)
            else -> null
        }

        if (text != null) {
            appendln("Content-Type: ${content.contentType}")
            appendln(text)
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
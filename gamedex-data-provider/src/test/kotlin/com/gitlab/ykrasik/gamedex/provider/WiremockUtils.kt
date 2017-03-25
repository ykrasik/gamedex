package com.gitlab.ykrasik.gamedex.provider

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.gitlab.ykrasik.gamedex.common.util.toJsonStr

/**
 * User: ykrasik
 * Date: 25/03/2017
 * Time: 14:51
 */

fun ResponseDefinitionBuilder.withContentTypeJson() = withHeader("Content-Type", "application/json")
fun ResponseDefinitionBuilder.withContentTypeBinary() = withHeader("Content-Type", "application/octet-stream")
fun ResponseDefinitionBuilder.withStringBody(content: String) = withBody(content).withHeader("Content-Length", content.length.toString())
fun ResponseDefinitionBuilder.withBinaryBody(content: ByteArray) = withBody(content).withHeader("Content-Length", content.size.toString())

fun aJsonResponse(content: Map<String, Any>) = aJsonResponse(content.toJsonStr())
fun aJsonResponse(content: String) = aResponse().withContentTypeJson().withStringBody(content)
fun aBinaryResponse(content: ByteArray) = aResponse().withContentTypeBinary().withBinaryBody(content)

/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.gitlab.ykrasik.gamedex.util.toJsonStr

/**
 * User: ykrasik
 * Date: 25/03/2017
 * Time: 14:51
 */
fun MappingBuilder.withHeader(name: String, value: String) = withHeader(name, equalTo(value))
fun MappingBuilder.withQueryParam(name: String, value: String) = withQueryParam(name, equalTo(value))
fun MappingBuilder.withBody(body: String) = withRequestBody(equalTo(body))

fun aJsonResponse(content: Any) = aJsonResponse(content.toJsonStr())
fun aJsonResponse(content: String) = aResponse().withContentTypeJson().withStringBody(content)
fun aBinaryResponse(content: ByteArray) = aResponse().withContentTypeBinary().withBinaryBody(content)

fun ResponseDefinitionBuilder.withContentTypeJson() = withHeader("Content-Type", "application/json")
fun ResponseDefinitionBuilder.withContentTypeBinary() = withHeader("Content-Type", "application/octet-stream")
fun ResponseDefinitionBuilder.withStringBody(content: String) = withBody(content).withHeader("Content-Length", content.length.toString())
fun ResponseDefinitionBuilder.withBinaryBody(content: ByteArray) = withBody(content).withHeader("Content-Length", content.size.toString())

fun RequestPatternBuilder.withHeader(name: String, value: String) = withHeader(name, equalTo(value))
fun RequestPatternBuilder.withQueryParam(name: String, value: String) = withQueryParam(name, equalTo(value))
fun RequestPatternBuilder.withBody(body: String) = withRequestBody(equalTo(body))
fun RequestPatternBuilder.withBodyContaining(body: String) = withRequestBody(containing(body))
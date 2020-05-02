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

import com.gitlab.ykrasik.gamedex.util.objectMapper
import io.ktor.client.call.TypeInfo
import io.ktor.client.features.json.JsonSerializer
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.readText

/**
 * User: ykrasik
 * Date: 16/06/2019
 * Time: 08:38
 */
object JacksonSerializer : JsonSerializer {
    override fun write(data: Any, contentType: ContentType): OutgoingContent =
        TextContent(objectMapper.writeValueAsString(data), contentType)

    override fun read(type: TypeInfo, body: Input): Any {
        return objectMapper.readValue(body.readText(), objectMapper.typeFactory.constructType(type.reifiedType))
    }
}
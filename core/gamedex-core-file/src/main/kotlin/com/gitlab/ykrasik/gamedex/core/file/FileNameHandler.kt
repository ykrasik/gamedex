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

package com.gitlab.ykrasik.gamedex.core.file

import com.gitlab.ykrasik.gamedex.FolderName

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 13:54
 */
object FileNameHandler {
    private val orderRegex = "^\\[\\d+\\]".toRegex()
    private val versionRegex = "\\[(?i:Alpha|Beta|Update|[A-Za-z])? ?v? ?[\\d\\.]*[A-Za-z]?[\\d\\.]*\\]".toRegex()
    private val metaTagRegex = "\\[.*?\\]".toRegex()
    private val spacesRegex = "\\s+".toRegex()

    fun analyze(rawName: String): FolderName {
        val (rawNameWithoutOrder, order) = extractMetadata(rawName, orderRegex)
        val (rawNameWithoutVersion, version) = extractMetadata(rawNameWithoutOrder, versionRegex)
        val (rawNameWithoutMetadata, metaTag) = extractMetadata(rawNameWithoutVersion, metaTagRegex)

        return FolderName(
            rawName = rawName,
            processedName = rawNameWithoutMetadata.collapseSpaces().replace(" - ", ": "),
            order = order,
            metaTag = metaTag,
            version = version
        )
    }

    private fun extractMetadata(rawName: String, regex: Regex): Pair<String, String?> {
        val match = regex.find(rawName) ?: return rawName to null
        val metadata = match.value.let { it.substring(1, it.length - 1) }
        val rawNameWithoutMetadata = rawName.removeRange(match.range)
        return rawNameWithoutMetadata to metadata
    }

    fun sanitizeFileName(name: String) = name.replace(": ", " - ")
        .replace("/", " ")
        .replace("\\", " ")
        .replace("?", " ")
        .collapseSpaces()

    private fun String.collapseSpaces() = spacesRegex.replace(this, " ").trim()
}
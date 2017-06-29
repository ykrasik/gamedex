package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.FolderMetadata

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 13:54
 */
object NameHandler {
    private val orderRegex = "^\\[\\d*\\]".toRegex()
    private val versionRegex = "\\[(?i:Alpha|Beta|Update|[A-Za-z])? ?v? ?[\\d\\.]*[A-Za-z]?[\\d\\.]*\\]".toRegex()
    private val metaTagRegex = "\\[.*?\\]".toRegex()
    private val spacesRegex = "\\s+".toRegex()

    fun analyze(rawName: String): FolderMetadata {
        val (rawNameWithoutOrder, order) = extractMetadata(rawName, orderRegex)
        val (rawNameWithoutVersion, version) = extractMetadata(rawNameWithoutOrder, versionRegex)
        val (rawNameWithoutMetadata, metaTag) = extractMetadata(rawNameWithoutVersion, metaTagRegex)

        return FolderMetadata(
            rawName = rawName,
            gameName = rawNameWithoutMetadata.collapseSpaces(),
            order = order?.toInt(),
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

    fun fromFileName(name: String) = name.collapseSpaces().replace(" - ", ": ")
    fun toFileName(name: String) = name.replace(": ", " - ")
        .replace("/", " ")
        .replace("\\", " ")
        .replace("?", " ")
        .collapseSpaces()

    private fun String.collapseSpaces() = spacesRegex.replace(this, " ").trim()
}
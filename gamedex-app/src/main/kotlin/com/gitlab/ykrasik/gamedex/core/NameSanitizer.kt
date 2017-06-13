package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.FolderMetaData

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 13:54
 */
object NameHandler {
    private val metaTagRegex = "(\\[.*?\\])".toRegex()
    private val versionRegex = "(\\[(?i:Alpha|Beta|Update|[A-Za-z])? ?v? ?[\\d\\.]*[A-Za-z]?[\\d\\.]*\\])".toRegex()
    private val spacesRegex = "\\s+".toRegex()

    fun analyze(rawName: String): FolderMetaData {
        val (rawNameWithoutVersion, version) = extractMetaData(rawName, versionRegex)
        val (rawNameWithoutMetaData, metaTag) = extractMetaData(rawNameWithoutVersion, metaTagRegex)

        return FolderMetaData(
            rawName = rawName,
            gameName = rawNameWithoutMetaData.collapseSpaces(),
            metaTag = metaTag,
            version = version
        )
    }

    private fun extractMetaData(rawName: String, regex: Regex): Pair<String, String?> {
        val match = regex.find(rawName) ?: return rawName to null
        val metaData = match.value.let { it.substring(1, it.length - 1) }
        val rawNameWithoutMetaData = rawName.removeRange(match.range)
        return rawNameWithoutMetaData to metaData
    }

    fun fromFileName(name: String) = name.replace(" - ", ": ").collapseSpaces()
    fun toFileName(name: String) = name.replace(": ", " - ")
        .replace("/", " ")
        .replace("\\", " ")
        .replace("?", " ")
        .collapseSpaces()

    private fun String.collapseSpaces() = spacesRegex.replace(this, " ").trim()
}
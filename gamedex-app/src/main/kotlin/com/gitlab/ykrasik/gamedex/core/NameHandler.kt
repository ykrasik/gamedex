package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.FolderMetaData

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

    fun analyze(rawName: String): FolderMetaData {
        val (rawNameWithoutOrder, order) = extractMetaData(rawName, orderRegex)
        val (rawNameWithoutVersion, version) = extractMetaData(rawNameWithoutOrder, versionRegex)
        val (rawNameWithoutMetaData, metaTag) = extractMetaData(rawNameWithoutVersion, metaTagRegex)

        return FolderMetaData(
            rawName = rawName,
            gameName = rawNameWithoutMetaData.collapseSpaces(),
            order = order?.toInt(),
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

    fun fromFileName(name: String) = name.collapseSpaces().replace(" - ", ": ")
    fun toFileName(name: String) = name.replace(": ", " - ")
        .replace("/", " ")
        .replace("\\", " ")
        .replace("?", " ")
        .collapseSpaces()

    private fun String.collapseSpaces() = spacesRegex.replace(this, " ").trim()
}